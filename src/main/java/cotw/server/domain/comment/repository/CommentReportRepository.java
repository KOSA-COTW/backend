package cotw.server.domain.comment.repository;

import cotw.server.domain.comment.entity.CommentReport;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {

    boolean existsByCommentIdAndMemberId(Long commentId, Long memberId);

    @Query("""
       select count(r) from CommentReport r
        where r.member.id = :memberId
          and r.createdAt >= :start and r.createdAt < :end
    """)
    long countDailyByMember(@Param("memberId") Long memberId,
                            @Param("start") LocalDateTime start,
                            @Param("end") LocalDateTime end);

    @Query("select count(r) from CommentReport r where r.comment.id = :commentId and r.clearedAt is null")
    long countActiveByCommentId(@Param("commentId") Long commentId);

    @Query("""
       select r.reason, count(r)
         from CommentReport r
        where r.comment.id = :commentId and r.clearedAt is null
        group by r.reason
    """)
    List<Object[]> countActiveByReason(@Param("commentId") Long commentId);

    @EntityGraph(attributePaths = {"member"})
    List<CommentReport> findByCommentIdAndClearedAtIsNullOrderByCreatedAtDesc(Long commentId);

    void deleteByMemberIdIn(Collection<Long> memberIds);

    @Query("select r.reason, count(r) from CommentReport r where r.comment.id = :commentId group by r.reason")
    List<Object[]> countByReason(@Param("commentId") Long commentId);

    @Query("select max(r.createdAt) from CommentReport r where r.comment.id = :commentId and r.clearedAt is null")
    Optional<LocalDateTime> findLastActiveReportedAt(@Param("commentId") Long commentId);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("""
        select r.comment.id, max(r.createdAt)
          from CommentReport r
         where r.clearedAt is null
         group by r.comment.id
         order by max(r.createdAt) desc
    """)
    List<Object[]> findRecentCommentIds(Pageable pageable);

    CommentReport findFirstByCommentIdOrderByCreatedAtAsc(Long commentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update CommentReport r set r.clearedAt = CURRENT_TIMESTAMP where r.comment.id = :commentId and r.clearedAt is null")
    int clearByCommentId(@Param("commentId") Long commentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update CommentReport r set r.clearedAt = CURRENT_TIMESTAMP where r.comment.id in :ids and r.clearedAt is null")
    int clearByCommentIdIn(@Param("ids") List<Long> ids);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from CommentReport r where r.comment.id = :commentId")
    void hardDeleteByCommentId(@Param("commentId") Long commentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from CommentReport r where r.comment.id in :ids")
    void hardDeleteByCommentIdIn(@Param("ids") List<Long> ids);

    // ✅ 미처리 신고 전체 개수
    @Query("select count(r) from CommentReport r where r.clearedAt is null")
    long countByClearedAtIsNull();
}
