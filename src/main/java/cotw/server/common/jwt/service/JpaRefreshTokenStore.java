package cotw.server.common.jwt.service;

import cotw.server.common.jwt.entity.RefreshToken;
import cotw.server.common.jwt.repository.RefreshTokenJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JpaRefreshTokenStore {

    private final RefreshTokenJpaRepository repo;
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC);

    public void save(String email, String refreshToken, Duration ttl) {
        String hash = hash(refreshToken);
        Instant exp = Instant.now().plus(ttl);
        RefreshToken rt = new RefreshToken();
        rt.setEmail(email);
        rt.setTokenHash(hash);
        rt.setExpiryDate(ISO.format(exp));
        repo.save(rt);
    }

    public boolean exists(String refreshToken) {
        return repo.existsByTokenHash(hash(refreshToken));
    }

    public Optional<RefreshToken> findByToken(String refreshToken) {
        return repo.findByTokenHash(hash(refreshToken));
    }

    @Transactional
    public void revoke(String refreshToken) {
        repo.deleteByTokenHash(hash(refreshToken));
    }

    @Transactional
    public void revokeAllByUser(String email) {
        repo.deleteAllByEmail(email);
    }

    private static String hash(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
