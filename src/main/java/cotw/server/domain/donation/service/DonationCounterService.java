package cotw.server.domain.donation.service;

import cotw.server.domain.payment.repository.PaymentLedgerRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DonationCounterService {
    private final StringRedisTemplate redis;
    private final RedisScript<Long> multiIncrScript;
    private final PaymentLedgerRepository repo;   // 주입

    private static String totalKey() { return "donation:total"; }
    private static String postKey(Long postId) { return "donation:post:" + postId + ":total"; }
    private static String dailyKey(LocalDate date) {
        return "donation:daily:" + DateTimeFormatter.BASIC_ISO_DATE.format(date) + ":total";
    }

    /** 키 존재 여부 */
    private boolean hasKey(String key) {
        return Boolean.TRUE.equals(redis.hasKey(key));
    }

    /** 값이 숫자(long)인지 빠르게 검증 */
    private boolean isNumericValue(String key) {
        try {
            String v = redis.opsForValue().get(key);
            if (v == null) return false;
            Long.parseLong(v);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    /** 이벤트 직전: 영향 키(전체/포스트/일자)를 DB 기준으로 on-demand 채움 */
    private void ensureInitialized(Long postId, LocalDate paidDate) {
        // 전체 합
        String tk = totalKey();
        if (!isNumericValue(tk)) {
            Long s = repo.sumDoneAll();
            redis.opsForValue().set(tk, String.valueOf(s));
        }
        // 포스트 합
        String pk = postKey(postId);
        if (!isNumericValue(pk)) {
            Long s = repo.sumDoneByPost(postId);
            redis.opsForValue().set(pk, String.valueOf(s));
        }
        // 일자 합
        String dk = dailyKey(paidDate);
        if (!isNumericValue(dk)) {
            LocalDateTime start = paidDate.atStartOfDay();
            LocalDateTime end   = paidDate.plusDays(1).atStartOfDay();
            Long s = repo.sumDoneByDateRange(start, end);
            redis.opsForValue().set(dk, String.valueOf(s));
        }
    }

    /** 확정 결제 반영(증가) */
    public void applyPaid(Long postId, long amountWon, LocalDate paidDate) {
        ensureInitialized(postId, paidDate); // 먼저 부분 리빌드
        List<@NotNull String> keys = List.of(totalKey(), postKey(postId), dailyKey(paidDate));
        redis.execute(multiIncrScript, keys, String.valueOf(amountWon));
    }

    /** 취소/환불 반영(감소) */
    public void applyReversal(Long postId, long amountWon, LocalDate paidDate) {
        ensureInitialized(postId, paidDate); // 먼저 부분 리빌드
        List<@NotNull String>  keys = List.of(totalKey(), postKey(postId), dailyKey(paidDate));
        redis.execute(multiIncrScript, keys, String.valueOf(-amountWon));
    }

    public long getTotal() {
        String v = redis.opsForValue().get(totalKey());
        return v == null ? 0L : Long.parseLong(v);
    }

    /** 전체 키 없으면 한 번만 전체 리빌드 트리거할지 여부 판단용 */
    public boolean hasTotalKey() { return hasKey(totalKey()); }
}
