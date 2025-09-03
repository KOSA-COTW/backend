package cotw.server.domain.comment.repository;

import cotw.server.domain.comment.entity.CommentReport;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {

    // 동일 댓글/사용자 신고 여부(빠른 UX 용도)
    boolean existsByCommentIdAndMemberId(Long commentId, Long memberId);

    // 하루 신고 횟수 집계([start, end) 반개구간)
    @Query("""
       select count(r) from CommentReport r
        where r.member.id = :memberId
          and r.createdAt >= :start and r.createdAt < :end
    """)
    long countDailyByMember(@Param("memberId") Long memberId,
                            @Param("start") LocalDateTime start,
                            @Param("end") LocalDateTime end);


    // 댓글의 총 신고 수
    long countByCommentId(Long commentId);

    @Query("select r.reason, count(r) from CommentReport r where r.comment.id = :commentId group by r.reason")
    List<Object[]> countByReason(@Param("commentId") Long commentId);

    @EntityGraph(attributePaths = {"member"}) // 엔티티에 'member' 필드 있어야 함
    List<CommentReport> findByCommentIdOrderByCreatedAtAsc(Long commentId);

    @Query("select max(r.createdAt) from CommentReport r where r.comment.id = :commentId")
    Optional<LocalDateTime> findLastReportedAt(@Param("commentId") Long commentId);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // ✅ 최초 신고 1건
    CommentReport findFirstByCommentIdOrderByCreatedAtAsc(Long commentId);

    // ✅ 댓글의 신고 로그 일괄 삭제(단건)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from CommentReport r where r.comment.id = :commentId")
    void deleteByCommentId(@Param("commentId") Long commentId);

    // ✅ 댓글의 신고 로그 일괄 삭제(벌크)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from CommentReport r where r.comment.id in :ids")
    void deleteByCommentIdIn(@Param("ids") List<Long> ids);
}
