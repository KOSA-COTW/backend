package cotw.server.domain.board.dto.response;

import cotw.server.domain.board.entity.Post;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Getter
public class PostListResponseDto {
    // 기본 정보
    private final Long id;
    private final String title;
    private final String category;
    private final String image;      // 대표 이미지 URL(없으면 null)
    private final LocalDateTime createdAt;

    // 금액 관련
    private final int target;        // 목표 금액
    private final int currentAmount;        // 현재 모금액
    private final int remaining;     // 남은 금액(음수면 0)
    private final int overfunded;    // 초과 모금액(<=0이면 0)
    private final double percent;    // 게이지용 0~100, 소수점 1자리
    private final double percentRaw; // 실제 달성률(100 초과 가능)

    // 상태 및 기간
    private final String status;     // "ONGOING" | "COMPLETED"
    private final LocalDate deadline;
    private final int daysLeft;      // 오늘 기준 남은 일수(음수면 0)
    private final int donorCount;

    public PostListResponseDto(Post post) {
        this(post, LocalDate.now());
    }

    public PostListResponseDto(Post post, LocalDate today) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.category = post.getCategory().getDisplayName();
        this.target = post.getAmount();
        this.currentAmount = post.getCurrentAmount();
        this.deadline = post.getDeadline();
        this.createdAt = post.getCreatedAt();
        this.donorCount = post.getDonorCount();
        // 남은 금액(음수 방지)
        int rawRemaining = target - currentAmount;
        this.remaining = Math.max(rawRemaining, 0);

        // 실제 달성률과 게이지용 퍼센트
        double rawPct = (target > 0) ? (currentAmount * 100.0) / target : 0.0;
        this.percentRaw = Math.round(rawPct * 10.0) / 10.0;      // 예: 123.4
        this.percent    = Math.min(this.percentRaw, 100.0);      // 게이지는 0~100으로 캡

        // 초과 모금액
        this.overfunded = Math.max(currentAmount - target, 0);

        this.status = post.getStatus();

        // 남은 일수(음수면 0)
        if (deadline != null) {
            long d = ChronoUnit.DAYS.between(today, deadline);
            this.daysLeft = (int) Math.max(d, 0);
        } else {
            this.daysLeft = 0;
        }

        // 대표 이미지(첫 번째)
        this.image = (post.getImages() == null || post.getImages().isEmpty())
                ? null
                : post.getImages().get(0).getImageUrl();
    }
}
