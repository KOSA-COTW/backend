package cotw.server.domain.comment.repository;

import cotw.server.domain.comment.entity.CommentReport;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {
    boolean existsByCommentIdAndMemberId(Long commentId, Long memberId);

    @Query("""
      select count(r) from CommentReport r
      where r.member.id = :memberId
        and r.createdAt >= :startOfDay and r.createdAt < :endOfDay
    """)
    long countTodayByMember(@Param("memberId") Long memberId,
                            @Param("startOfDay") LocalDateTime startOfDay,
                            @Param("endOfDay") LocalDateTime endOfDay);
}
