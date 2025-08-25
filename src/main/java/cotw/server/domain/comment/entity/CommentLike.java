package cotw.server.domain.comment.entity;

import cotw.server.common.BaseEntity;   // ✅ BaseEntity import
import cotw.server.domain.member.entity.Member;
import cotw.server.domain.board.entity.Comment;
import jakarta.persistence.*;
import lombok.*;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "comment_like",
        uniqueConstraints = @UniqueConstraint(name = "uq_comment_like",
                columnNames = {"comment_id", "member_id"}),
        indexes = {
                @Index(name = "idx_like_comment", columnList = "comment_id"),
                @Index(name = "idx_like_member", columnList = "member_id")
        })
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor @Builder
public class CommentLike extends BaseEntity {   // ✅ 공통 엔티티 상속

    @Id @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;


}
