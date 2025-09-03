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
                // 목록 조회 최적화: 게시글 단위로 최신/생성순
                @Index(name = "idx_comment_post_created", columnList = "post_id, created_at"),
                // 공감순 정렬(정렬방향은 쿼리에서 지정, 인덱스는 컬럼만)
                @Index(name = "idx_comment_post_like", columnList = "post_id, like_count, id"),
                // 48h 검토 만료 배치/스케줄러용
                @Index(name = "idx_comment_moderation_due", columnList = "moderation_due_at")
        })
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor @Builder
public class Comment extends BaseEntity {

    @Id @GeneratedValue(strategy = IDENTITY)
    private Long id;

    // 작성자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 소속 게시글
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    // 본문
    @Column(nullable = false, length = 1000)
    private String content;

    // 좋아요 수
    @Column(name = "like_count", nullable = false)
    @Builder.Default
    private int likeCount = 0;

    // 신고 수
    @Column(name = "report_count", nullable = false)
    @Builder.Default
    private int reportCount = 0;

    // 공개 여부(임시 숨김 시 false)
    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private boolean isPublic = true;

    // 소프트 삭제 시각
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // 임시 숨김 후 48시간 검토 마감 시각
    @Column(name = "moderation_due_at")
    private LocalDateTime moderationDueAt;

    // 낙관적 락(동시 업데이트 감지)
    // - @Builder.Default + NOT NULL: 빌더로 생성 시 null 방지
    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    @Column(name = "admin_memo", length = 500)
    private String adminMemo;

    // ===== 도메인 로직 =====

    /** 삭제 여부 */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /** 소프트 삭제 */
    public void delete() {
        this.deletedAt = LocalDateTime.now();
        this.isPublic = false; // 소프트 삭제 시 비공개 전환
    }

    /** 좋아요 +1 */
    public void increaseLikeCount() { this.likeCount++; }

    /** 좋아요 -1 (최소 0) */
    public void decreaseLikeCount() { if (this.likeCount > 0) this.likeCount--; }


    public void applyReportTally(int total) {
        this.reportCount = total;
        if (this.reportCount >= 3 && this.isPublic) {
            this.isPublic = false;
            this.moderationDueAt = LocalDateTime.now().plusHours(48);
        }
    }

    /** (과거 코드 호환용) 증가 방식 사용 시에도 동일 규칙 적용 */
    @Deprecated
    public void increaseReportCount() { applyReportTally(this.reportCount + 1); }

    /** 관리자 복원: 신고수 초기화 + 공개 + 삭제/검토 마감 초기화 */
    public void restoreByAdmin() {
        this.reportCount = 0;
        this.isPublic = true;
        this.deletedAt = null;
        this.moderationDueAt = null;
    }


    public void makePrivate() { this.isPublic = false; }


    public void makePublic() { this.isPublic = true; this.moderationDueAt = null; }


    public boolean isModerationExpired() {
        return moderationDueAt != null && LocalDateTime.now().isAfter(moderationDueAt);
    }
}
