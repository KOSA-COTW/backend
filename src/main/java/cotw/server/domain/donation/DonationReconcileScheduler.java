package cotw.server.domain.donation;

import cotw.server.domain.payment.repository.PaymentOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DonationReconcileScheduler {

    private final PaymentOrderRepository orderRepo;
    private final StringRedisTemplate redis;

    @Value("${app.reconcile.window-days:14}")
    private int windowDays;

    @Value("${app.redis.donation.total-key:donation:total}")
    private String totalKey;

    @Value("${app.timezone:Asia/Seoul}")
    private String zoneId;

    @Scheduled(cron = "0 47 17 * * *") // 매일 자정에 실행
    @Transactional
    public void reconcileTotalDonation() {
        ZoneId zone = ZoneId.of(zoneId);
        LocalDate today = LocalDate.now(zone);

        LocalDateTime endExclusive = today.plusDays(1).atStartOfDay();

        // 전체 기부금 총액 집계 (DONE - CANCELED)
        List<PaymentOrderRepository.TotalDonationRow> rows = orderRepo.aggregateTotalDonationByDay();

        // 기부금 합계
        long totalDonation = rows.stream().mapToLong(r -> Optional.ofNullable(r.getNetAmount()).orElse(0L)).sum();

        // Redis에 총합을 갱신
        try {
            redis.executePipelined((RedisCallback<Object>) conn -> {
                byte[] key = totalKey.getBytes(StandardCharsets.UTF_8);
                conn.del(key);  // 기존 총합을 삭제하고 새로 덮어씁니다.
                conn.set(key, String.valueOf(totalDonation).getBytes(StandardCharsets.UTF_8));  // 총합을 갱신합니다.
                return null;
            });
            if (log.isDebugEnabled()) {
                log.debug("Reconciled total donation: {} into key={}", totalDonation, totalKey);
            }
        } catch (DataAccessException e) {
            log.warn("Redis reconcile failed for total donation, reason={}", e.getMessage());
        }
    }
}
