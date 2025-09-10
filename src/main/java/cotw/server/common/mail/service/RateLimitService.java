package cotw.server.common.mail.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate stringRedisTemplate;

    public void checkOrThrow(String key, Duration window, int maxHits) {
        // 1) 카운트 + TTL 설정(첫 히트일 때만)
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, window);
        }

        // 2) 초과 시 예외
        if (count != null && count > maxHits) {
            long ttl = Optional.ofNullable(stringRedisTemplate.getExpire(key, TimeUnit.SECONDS)).orElse(0L);
            throw new RateLimitExceededException("Too many requests");
        }
    }

    public void hitOrThrow(String bucket, int limit, Duration window) {
        String key = "rl:" + bucket + ":" + window.getSeconds();
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, window);
        }
        if (count != null && count > limit) {
            throw new RateLimitExceededException("Too many requests");
        }
    }

    public static class RateLimitExceededException extends RuntimeException {
        public RateLimitExceededException(String msg) { super(msg); }
    }
}
