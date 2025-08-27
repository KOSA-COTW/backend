package cotw.server.common;

import cotw.server.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class RetentionCleanupJob {
    private final MemberService memberService;

    // 매일 03:15 (서울) — 필요 시 조정
    @Scheduled(cron = "0 15 3 * * *", zone = "Asia/Seoul")
    public void purge() {
        int total = 0;
        int n;
        do {
            n = memberService.hardDeleteExpiredMembersChunk(500);
            total += n;
        } while (n > 0);
        log.info("Hard-deleted {} expired members", total);
    }
}