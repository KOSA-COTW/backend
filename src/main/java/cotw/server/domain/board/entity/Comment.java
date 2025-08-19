package cotw.server.domain.board.entity;

import cotw.server.common.BaseEntity;
import cotw.server.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "comment",
        indexes = {
                @Index(name = "idx_comment_post_created", columnList = "post_id, created_at DESC"),
                @Index(name = "idx_comment_post_like", columnList = "post_id, like_count DESC, id DESC")
        })
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor @Builder
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(name = "like_count", nullable = false)
    @Builder.Default
    private int likeCount = 0;

    @Column(name = "report_count", nullable = false)
    @Builder.Default
    private int reportCount = 0;

    // 요구사항: 신고 3회 시 임시 숨김
    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private boolean isPublic = true;

    // 소프트 삭제(관리자 삭제 등)
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // 임시 숨김 후 48시간 검토 마감 시각
    @Column(name = "moderation_due_at")
    private LocalDateTime moderationDueAt;

    // 비즈니스 메서드들
    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
        this.isPublic = false; // 소프트 삭제 시 비공개 전환까지 일괄 처리
    }

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    public void increaseReportCount() {
        this.reportCount++;
        // 신고 "도달 시점"에만 48h 마감 설정 (중복 설정 방지)
        if (this.reportCount == 3) {
            this.isPublic = false;
            this.moderationDueAt = LocalDateTime.now().plusHours(48);
        }
    }

    public void makePublic() {
        this.isPublic = true;
        this.moderationDueAt = null;
    }

    public void makePrivate() {
        this.isPublic = false;
    }

    public boolean isModerationExpired() {
        return moderationDueAt != null && LocalDateTime.now().isAfter(moderationDueAt);
    }
        /** 관리자 복원: 신고수 초기화 + 공개 + 삭제/검토 마감 초기화 */
        public void restoreByAdmin () {
            this.reportCount = 0;
            this.isPublic = true;
            this.deletedAt = null;
            this.moderationDueAt = null;
        }
    }

