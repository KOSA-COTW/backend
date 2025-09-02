package cotw.server.domain.comment.repository;

import cotw.server.domain.comment.entity.CommentReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;


public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {

    // 동일 댓글/사용자 신고 여부(빠른 UX 용도)
    boolean existsByCommentIdAndMemberId(Long commentId, Long memberId);

    // 하루 신고 횟수 집계([start, end) 반개구간)
    @Query("""
       select count(r) from CommentReport r
        where r.member.id = :memberId
          and r.createdAt >= :start and r.createdAt < :end
    """)
    long countDailyByMember(Long memberId, LocalDateTime start, LocalDateTime end);

    // 댓글의 총 신고 수
    long countByCommentId(Long commentId);

    void deleteByMemberIdIn(Collection<Long> memberIds);
}
