package cotw.server.domain.board.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import cotw.server.domain.board.entity.Category;
import cotw.server.domain.board.entity.Image;
import cotw.server.domain.board.entity.Post;
import cotw.server.domain.board.entity.PostVisibility;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Getter
public class PostResponseDto {

    // 기본 정보
    private Long id;
    private String title;
    private String content;
    private String authorName;
    private String authorEmail;
    private Category category;
    private List<String> imageUrls;

    // 게시글 상태
    private PostVisibility visibilityStatus;
    private String rejectionReason;

    // 날짜 정보
    private LocalDate deadline;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime updatedAt;

    // 금액 관련 (계산된 값들)
    private int amount;           // 목표 금액
    private int currentAmount;    // 현재 모금액
    private int remaining;        // 남은 금액 (음수면 0)
    private int overfunded;       // 초과 모금액 (<=0이면 0)
    private double percent;       // 게이지용 퍼센트 (0~100, 소수점 1자리)
    private double percentRaw;    // 실제 달성률 (100 초과 가능)

    // 상태 정보
    private String status;        // "ONGOING" | "COMPLETED"
    private int daysLeft;         // 남은 일수 (음수면 0)
    private int donorCount;       // 기부자 수

    public PostResponseDto(Post post) {
        this(post, LocalDate.now());
    }

    public PostResponseDto(Post post, LocalDate today) {
        // 기본 정보
        this.id = post.getId();
        this.title = post.getTitle();
        this.content = post.getContent();
        this.authorName = post.getAuthor().getName();
        this.authorEmail = post.getAuthor().getEmail();
        this.category = post.getCategory();
        this.deadline = post.getDeadline();
        this.createdAt = post.getCreatedAt();
        this.updatedAt = post.getUpdatedAt();

        // 공개 상태
        this.visibilityStatus = post.getVisibilityStatus();
        this.rejectionReason = post.getRejectionReason();

        // 이미지 URL 리스트
        List<String> imageUrls = new ArrayList<>();
        for (Image image : post.getImages()) {
            imageUrls.add(image.getImageUrl());
        }
        this.imageUrls = imageUrls;

        // 기부자 수
        this.donorCount = post.getDonorCount();

        // 금액 관련 계산
        this.amount = post.getAmount();
        this.currentAmount = post.getCurrentAmount();

        // 남은 금액 (음수 방지)
        int rawRemaining = amount - currentAmount;
        this.remaining = Math.max(rawRemaining, 0);

        // 달성률 계산
        double rawPct = (amount > 0) ? (currentAmount * 100.0) / amount : 0.0;
        this.percentRaw = Math.round(rawPct * 10.0) / 10.0;      // 예: 123.4%
        this.percent = Math.min(this.percentRaw, 100.0);         // 게이지는 0~100으로 캡

        // 초과 모금액
        this.overfunded = Math.max(currentAmount - amount, 0);

        // 상태 계산
        this.status = post.getStatus();

        // 남은 일수 계산
        if (deadline != null) {
            long d = ChronoUnit.DAYS.between(today, deadline);
            this.daysLeft = (int) Math.max(d, 0);
        } else {
            this.daysLeft = 0;
        }
    }
}