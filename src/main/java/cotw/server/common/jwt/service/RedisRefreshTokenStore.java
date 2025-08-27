package cotw.server.common.jwt.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenService {

    private final StringRedisTemplate redis;

    /** email 기반 사용자별 키세트 및 개별 토큰 키  */
    private String tokenKey(String tokenHash) { return "rt:token:" + tokenHash; }

    private String userSet(String email)       { return "rt:user:" + email; }

    private String hash(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public void save(String email, String refreshToken, Duration ttl) {
        String h = hash(refreshToken);
        ValueOperations<String, String> ops = redis.opsForValue();
        ops.set(tokenKey(h), email, ttl);
        redis.opsForSet().add(userSet(email), h);
        // user set은 별도 TTL 없이 관리(원하면 사용자 활동마다 주기적 청소 가능)
    }

    public boolean exists(String refreshToken) {
        String h = hash(refreshToken);
        return Boolean.TRUE.equals(redis.hasKey(tokenKey(h)));
    }

    public void revoke(String refreshToken) {
        String h = hash(refreshToken);
        String email = redis.opsForValue().get(tokenKey(h));
        redis.delete(tokenKey(h));
        if (email != null) redis.opsForSet().remove(userSet(email), h);
    }

    public void revokeAllByUser(String email) {
        String setKey = userSet(email);
        Set<String> members = redis.opsForSet().members(setKey);
        if (members != null) {
            for (String h : members) redis.delete(tokenKey(h));
        }
        redis.delete(setKey);
    }
}
