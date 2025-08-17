package cotw.server.domain.comment.entity;



import cotw.server.common.BaseEntity;
import cotw.server.domain.board.domain.Post;
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

    @Id @GeneratedValue(strategy = IDENTITY)
    private Long id;

    // 실제 프로젝트의 Member/Post 엔티티로 교체
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "post_id", nullable = false)
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

    public boolean isDeleted() { return deletedAt != null; }
}
