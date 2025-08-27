package cotw.server.common.jwt.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@Primary
@RequiredArgsConstructor
public class DualStoreRefreshTokenService implements RefreshTokenService {

    private final RedisRefreshTokenStore redis;     // Redis (이미 존재)
    private final JpaRefreshTokenStore jpa;    // DB 어댑터

    @Override
    @Transactional
    public void save(String email, String refreshToken, Duration ttl) {
        // 1) DB 기록(권위)
        jpa.save(email, refreshToken, ttl);
        // 2) Redis 기록(즉각 차단용)
        redis.save(email, refreshToken, ttl);
    }

    @Override
    public boolean exists(String refreshToken) {
        if (redis.exists(refreshToken)) return true;
        boolean inDb = jpa.exists(refreshToken);
        // Self-heal: DB엔 있는데 Redis엔 없으면 TTL 계산 후 재주입
        if (inDb) {
            jpa.findByToken(refreshToken).ifPresent(rt -> {
                Instant now = Instant.now();
                Instant exp = Instant.parse(rt.getExpiryDate());
                Duration remain = Duration.between(now, exp);
                if (!remain.isNegative()) {
                    redis.save(rt.getEmail(), refreshToken, remain);
                }
            });
        }
        return inDb;
    }

    @Override
    @Transactional
    public void revoke(String refreshToken) {
        redis.revoke(refreshToken);
        jpa.revoke(refreshToken);
    }

    @Override
    @Transactional
    public void revokeAllByUser(String email) {
        redis.revokeAllByUser(email);
        jpa.revokeAllByUser(email);
    }
}
