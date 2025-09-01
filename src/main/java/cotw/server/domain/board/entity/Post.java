package cotw.server.domain.board.entity;

import cotw.server.common.BaseEntity;
import cotw.server.domain.board.dto.request.PostUpdateRequestDto;
import cotw.server.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 글 작성자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member author;

    // 카테고리
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    // 총 목표 금액
    @Column(nullable = false)
    private int amount;

    // 현재까지 모금된 금액
    @Column(nullable = false)
    private int currentAmount;

    // 게시글 공개 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostVisibility visibilityStatus = PostVisibility.PRIVATE;

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    // 기부 마감일
    @Column(nullable=false)
    private LocalDate deadline;

    // 이미지 리스트
    @Builder.Default
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Image> images = new ArrayList<>();

    // 기부한 사용자 목록
    @Builder.Default
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Participant> participants  = new ArrayList<>();

    // 댓글 목록
    @Builder.Default
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    public void addImage(Image image) {
        this.images.add(image);
        image.setPost(this);
    }

    public void update(PostUpdateRequestDto dto) {
        if (dto.getTitle() != null) this.title = dto.getTitle();
        if (dto.getContent() != null) this.content = dto.getContent();
        if (dto.getCategory() != null) this.category = dto.getCategory();
        if (dto.getAmount() > 0) this.amount = dto.getAmount();
        // 승인 상태(PENDING/APPROVED)였다면 수정 후 자동으로 PRIVATE로 변경
        if (this.visibilityStatus == PostVisibility.APPROVED ||
                this.visibilityStatus == PostVisibility.PENDING) {
            this.visibilityStatus = PostVisibility.PRIVATE;
        }
        if (dto.getDeadline() != null && dto.getDeadline().isAfter(LocalDate.now())) {
            this.deadline = dto.getDeadline();
        }
    }

    public void approve() {
        this.visibilityStatus = PostVisibility.APPROVED;
    }

    public void reject(String reason) {
        this.visibilityStatus = PostVisibility.REJECTED;
        this.rejectionReason = reason;
    }

    public void markPending() {
        this.visibilityStatus = PostVisibility.PENDING;
    }

    public void makePrivate() {
        this.visibilityStatus = PostVisibility.PRIVATE;
    }

    public boolean isCompleted() {
        return this.deadline.isBefore(LocalDate.now()); // 목표 달성 여부와 관계없이 마감일만으로 판단
    }

    public String getStatus() {
        return isCompleted() ? "COMPLETED" : "ONGOING";
    }

    // 기부자 수
    public int getDonorCount() {
        return this.participants != null ? this.participants.size() : 0;
    }

    // 기부 금액 추가
    public void addDonationAmount(int donationAmount) {
        if (donationAmount > 0) {
            this.currentAmount += donationAmount;
        }
    }

    // 기부 금액 차감 (취소 시 사용)
    public void subtractDonationAmount(int donationAmount) {
        if (donationAmount > 0 && this.currentAmount >= donationAmount) {
            this.currentAmount -= donationAmount;
        }
    }

    // 기부 참여자 추가
    public void addParticipant(Participant participant) {
        this.participants.add(participant);

    }

    // 기존 이미지 전체 제거
    public void clearImages() {
        this.images.clear();
    }
}