package cotw.server.domain.comment.repository;

import cotw.server.domain.comment.entity.CommentReport;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {

    // 동일 댓글/사용자 신고 여부 (중복 신고 방지)
    boolean existsByCommentIdAndMemberId(Long commentId, Long memberId);

    // 하루 신고 횟수 집계 ([start, end), clearedAt 무시 → 일일 제한 용도)
    @Query("""
       select count(r) from CommentReport r
        where r.member.id = :memberId
          and r.createdAt >= :start and r.createdAt < :end
    """)
    long countDailyByMember(@Param("memberId") Long memberId,
                            @Param("start") LocalDateTime start,
                            @Param("end") LocalDateTime end);

    // ===== 활성 신고 집계 (clearedAt is null) =====
    @Query("select count(r) from CommentReport r where r.comment.id = :commentId and r.clearedAt is null")
    long countActiveByCommentId(@Param("commentId") Long commentId);

    @Query("""
       select r.reason, count(r)
         from CommentReport r
        where r.comment.id = :commentId and r.clearedAt is null
        group by r.reason
    """)
    List<Object[]> countActiveByReason(@Param("commentId") Long commentId);

    // ===== 활성 로그 조회 (확장행용) =====

    @EntityGraph(attributePaths = {"member"})
    List<CommentReport> findByCommentIdAndClearedAtIsNullOrderByCreatedAtDesc(Long commentId);

    void deleteByMemberIdIn(Collection<Long> memberIds);

    @Query("select r.reason, count(r) from CommentReport r where r.comment.id = :commentId group by r.reason")
    List<Object[]> countByReason(@Param("commentId") Long commentId);

    // (선택) 활성 로그의 마지막 신고 시각
    @Query("select max(r.createdAt) from CommentReport r where r.comment.id = :commentId and r.clearedAt is null")
    Optional<LocalDateTime> findLastActiveReportedAt(@Param("commentId") Long commentId);

    // 기간 카운트 (대시보드용)
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // 최초 신고 (전체 로그 기준)
    CommentReport findFirstByCommentIdOrderByCreatedAtAsc(Long commentId);

    // ===== 신고 초기화: '무효 처리' (clearedAt 업데이트) =====
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update CommentReport r set r.clearedAt = CURRENT_TIMESTAMP where r.comment.id = :commentId and r.clearedAt is null")
    int clearByCommentId(@Param("commentId") Long commentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update CommentReport r set r.clearedAt = CURRENT_TIMESTAMP where r.comment.id in :ids and r.clearedAt is null")
    int clearByCommentIdIn(@Param("ids") List<Long> ids);

    // ===== (선택) 하드 삭제 (정말 필요할 때만 사용) =====
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from CommentReport r where r.comment.id = :commentId")
    void hardDeleteByCommentId(@Param("commentId") Long commentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from CommentReport r where r.comment.id in :ids")
    void hardDeleteByCommentIdIn(@Param("ids") List<Long> ids);

}
