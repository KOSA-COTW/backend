package cotw.server.common.mail.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cotw.server.common.mail.tokenPayload.EmailTokenPayload;
import cotw.server.common.mail.tokenPayload.EmailTokenPurpose;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailTokenService {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    private static final SecureRandom RANDOM = new SecureRandom();

    enum VerifyResult { OK, MISMATCH, EXPIRED, INTERNAL_ERROR }

    private String key(String email, EmailTokenPurpose purpose) {
        return "email:token:%s:%s".formatted(purpose.name(), email);
    }

    public void save(String email, EmailTokenPurpose purpose, String code, Duration ttl) {
        redis.opsForValue().set(key(email, purpose), code, ttl);
    }

    public String create(Long memberId, String email, EmailTokenPurpose purpose,
                         Duration ttl, String ip, String ua) {
        String jti = uuidB64Url();
        String secret = randomB64Url(32);
        String token = jti + "." + secret;

        EmailTokenPayload payload = new EmailTokenPayload();
        payload.setMemberId(memberId);
        payload.setEmail(email);
        payload.setPurpose(purpose);
        payload.setExpiresAtEpochSec(Instant.now().plus(ttl).getEpochSecond());
        payload.setUsed(false);
        payload.setSecretHash(sha256(secret));
        payload.setIp(ip);
        payload.setUa(ua);

        String key = key(purpose, jti);
        try {
            String json = objectMapper.writeValueAsString(payload);
            redis.opsForValue().set(key, json, ttl);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        return token; // 이 값을 메일 링크에 포함
    }

    public Long consume(String token, EmailTokenPurpose expectedPurpose) {
        String[] parts = token.split("\\.");
        if (parts.length != 2) throw new IllegalArgumentException("Invalid token");

        String jti = parts[0];
        String secret = parts[1];

        String key = key(expectedPurpose, jti);
        String json = redis.opsForValue().get(key);
        if (json == null) throw new IllegalArgumentException("Token not found");

        try {
            EmailTokenPayload payload = objectMapper.readValue(json, EmailTokenPayload.class);
            if (payload.isUsed()) throw new IllegalArgumentException("Token already used");
            if (payload.getPurpose() != expectedPurpose) throw new IllegalArgumentException("Purpose mismatch");
            if (Instant.now().getEpochSecond() > payload.getExpiresAtEpochSec())
                throw new IllegalArgumentException("Token expired");

            // 상수시간 비교
            if (!constantTimeEquals(payload.getSecretHash(), sha256(secret)))
                throw new IllegalArgumentException("Bad token secret");

            // 사용 처리(DEL 또는 used=true로 마킹)
            payload.setUsed(true);
            redis.opsForValue().set(key, objectMapper.writeValueAsString(payload),
                    Duration.ofSeconds(payload.getExpiresAtEpochSec() - Instant.now().getEpochSecond()));

            // 원자성 강화가 필요하면 Lua 스크립트 사용 권장
            return payload.getMemberId();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    public VerifyResult verifyAndConsume(String email, EmailTokenPurpose purpose, String code) {
        String k = key(email, purpose);
        try {
            String stored = redis.opsForValue().get(k);
            if (stored == null) return VerifyResult.EXPIRED;
            if (!stored.equals(code)) return VerifyResult.MISMATCH;
            // 일치 → 1회성 소비
            redis.delete(k);
            return VerifyResult.OK;
        } catch (DataAccessException e) {
            return VerifyResult.INTERNAL_ERROR;
        }
    }

    private static String key(EmailTokenPurpose purpose, String jti) {
        return "email-token:" + purpose + ":" + jti;
    }
    private static String sha256(String v) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().withoutPadding().encodeToString(md.digest(v.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) { throw new RuntimeException(e); }
    }
    private static String uuidB64Url() {
        UUID u = UUID.randomUUID();
        ByteBuffer bb = ByteBuffer.allocate(16);
        bb.putLong(u.getMostSignificantBits());
        bb.putLong(u.getLeastSignificantBits());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bb.array());
    }
    private static String randomB64Url(int size) {
        byte[] b = new byte[size];
        RANDOM.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int res = 0;
        for (int i = 0; i < a.length(); i++) res |= a.charAt(i) ^ b.charAt(i);
        return res == 0;
    }
}
