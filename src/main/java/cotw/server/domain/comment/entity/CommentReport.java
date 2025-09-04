package cotw.server.domain.comment.entity;

import cotw.server.common.BaseEntity;
import cotw.server.domain.board.entity.Comment;
import cotw.server.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 댓글 신고 로그 엔티티
 * - UNIQUE(comment_id, member_id): 동일 사용자가 동일 댓글을 1회만 신고 가능
 */
@Entity
@Table(name = "comment_report",
        uniqueConstraints = @UniqueConstraint(name = "uq_comment_report",
                columnNames = {"comment_id","member_id"}),
        indexes = {
                @Index(name = "idx_report_comment", columnList = "comment_id"),
                @Index(name = "idx_report_member", columnList = "member_id"),
                // 일일 신고 3회 제한 집계 최적화를 위한 보조 인덱스
                @Index(name = "idx_report_member_created", columnList = "member_id, created_at")
        })
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@ToString(exclude = {"comment", "member"})   // 순환참조 방지
@EqualsAndHashCode(of = "id")                // 식별자 기준 동등성
public class CommentReport extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 신고 대상 댓글
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    // 신고자
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 신고 사유
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReportReason reason;

    // (선택) 기타 사유 상세
    @Column(length = 200)
    private String detail;

    /** 신고 무효 처리 시각(집계 제외) */
    @Column(name = "cleared_at")
    private LocalDateTime clearedAt;
}
