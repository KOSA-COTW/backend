package cotw.server.domain.admin.dto.response;

import cotw.server.domain.board.entity.Post;
import cotw.server.domain.board.entity.PostVisibility;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Getter
public class AdminPostListResponseDto {

    // 기본 정보
    private final Long id;
    private final String title;
    private final String category;
    private final String authorName;     // 작성자 이름
    private final String authorEmail;    // 작성자 이메일 (관리자만 볼 수 있음)
    private final String image;          // 대표 이미지 URL (없으면 null)
    private final LocalDateTime createdAt;

    // 공개 상태
    private final PostVisibility visibilityStatus;
    private final String rejectionReason; // 반려 사유

    // 금액 관련
    private final int amount;            // 목표 금액
    private final int currentAmount;     // 현재 모금액
    private final int remaining;         // 남은 금액(음수면 0)
    private final int overfunded;        // 초과 모금액(<=0이면 0)
    private final double percent;        // 게이지용 0~100, 소수점 1자리
    private final double percentRaw;     // 실제 달성률(100 초과 가능)

    // 상태 및 기간
    private final String status;         // "ONGOING" | "COMPLETED"
    private final LocalDate deadline;
    private final int daysLeft;          // 오늘 기준 남은 일수(음수면 0)
    private final int donorCount;

    public AdminPostListResponseDto(Post post) {
        this(post, LocalDate.now());
    }

    public AdminPostListResponseDto(Post post, LocalDate today) {
        // 기본 정보
        this.id = post.getId();
        this.title = post.getTitle();
        this.category = post.getCategory().getDisplayName();
        this.authorName = post.getAuthor().getName();
        this.authorEmail = post.getAuthor().getEmail();
        this.createdAt = post.getCreatedAt();

        // 공개 상태
        this.visibilityStatus = post.getVisibilityStatus();
        this.rejectionReason = post.getRejectionReason();

        // 금액 관련
        this.amount = post.getAmount();
        this.currentAmount = post.getCurrentAmount();

        int rawRemaining = amount - currentAmount;
        this.remaining = Math.max(rawRemaining, 0);

        double rawPct = (amount > 0) ? (currentAmount * 100.0) / amount : 0.0;
        this.percentRaw = Math.round(rawPct * 10.0) / 10.0;   // 예: 123.4
        this.percent    = Math.min(this.percentRaw, 100.0);   // 게이지 캡

        this.overfunded = Math.max(currentAmount - amount, 0);

        // 상태
        this.status = post.getStatus();
        this.deadline = post.getDeadline();

        if (deadline != null) {
            long d = ChronoUnit.DAYS.between(today, deadline);
            this.daysLeft = (int) Math.max(d, 0);
        } else {
            this.daysLeft = 0;
        }

        this.donorCount = post.getDonorCount();

        // 대표 이미지
        this.image = (post.getImages() == null || post.getImages().isEmpty())
                ? null
                : post.getImages().get(0).getImageUrl();
    }
}