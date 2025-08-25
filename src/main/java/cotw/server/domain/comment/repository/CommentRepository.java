// src/main/java/cotw/server/domain/comment/repository/CommentRepository.java
package cotw.server.domain.comment.repository;

import cotw.server.domain.board.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // ===== 최신순 (기존) =====
    @Query(value = """
        select c
        from Comment c
        where c.post.id = :postId
          and (
                :admin = true
             or (
                   c.deletedAt is null
               and ( c.isPublic = true
                     or ( :viewerId is not null and c.member.id = :viewerId )
                   )
                )
          )
        order by c.createdAt desc, c.id desc
      """,
            countQuery = """
        select count(c)
        from Comment c
        where c.post.id = :postId
          and (
                :admin = true
             or (
                   c.deletedAt is null
               and ( c.isPublic = true
                     or ( :viewerId is not null and c.member.id = :viewerId )
                   )
                )
          )
      """
    )
    Page<Comment> findLatestVisible(@Param("postId") Long postId,
                                    @Param("viewerId") Long viewerId,
                                    @Param("admin") boolean admin,
                                    Pageable pageable);

    // ===== 공감순 (기존) =====
    @Query(value = """
        select c
        from Comment c
        where c.post.id = :postId
          and (
                :admin = true
             or (
                   c.deletedAt is null
               and ( c.isPublic = true
                     or ( :viewerId is not null and c.member.id = :viewerId )
                   )
                )
          )
        order by c.likeCount desc, c.id desc
      """,
            countQuery = """
        select count(c)
        from Comment c
        where c.post.id = :postId
          and (
                :admin = true
             or (
                   c.deletedAt is null
               and ( c.isPublic = true
                     or ( :viewerId is not null and c.member.id = :viewerId )
                   )
                )
          )
      """
    )
    Page<Comment> findLikeVisible(@Param("postId") Long postId,
                                  @Param("viewerId") Long viewerId,
                                  @Param("admin") boolean admin,
                                  Pageable pageable);

    // ===== 비정규화 카운터: 원자적 증가/감소 =====
    // flush 즉시 수행 + 1차 캐시 자동 클리어로 stale 방지
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Comment c set c.likeCount = c.likeCount + 1 where c.id = :commentId")
    int incrementLikeCount(@Param("commentId") Long commentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Comment c
           set c.likeCount = case when c.likeCount > 0 then c.likeCount - 1 else 0 end
         where c.id = :commentId
    """)
    int decrementLikeCount(@Param("commentId") Long commentId);


    // ====== 관리자 전용 조회 ======
// 상태 정의
// PENDING : 숨김 & 삭제 아님 & 검토기한 >= now
// EXPIRED : 숨김 & 삭제 아님 & 검토기한  < now
// HIDDEN  : 숨김(삭제 포함) 전체
// ALL     : 신고 관련 모든 것 (reportCount>0 or 숨김 or 검토기한 설정됨)

    @Query(value = """
    select c from Comment c
    where c.isPublic = false
      and c.deletedAt is null
      and (c.moderationDueAt is not null and c.moderationDueAt >= CURRENT_TIMESTAMP)
      and (:reason is null or exists (
            select 1 from CommentReport r
             where r.comment.id = c.id and r.reason = :reason
      ))
      and (:from is null or exists (
            select 1 from CommentReport r2
             where r2.comment.id = c.id and r2.createdAt >= :from
      ))
      and (:to is null or exists (
            select 1 from CommentReport r3
             where r3.comment.id = c.id and r3.createdAt < :to
      ))
    order by c.moderationDueAt asc, c.id asc
""",
            countQuery = """
    select count(c) from Comment c
    where c.isPublic = false
      and c.deletedAt is null
      and (c.moderationDueAt is not null and c.moderationDueAt >= CURRENT_TIMESTAMP)
      and (:reason is null or exists (
            select 1 from CommentReport r
             where r.comment.id = c.id and r.reason = :reason
      ))
      and (:from is null or exists (
            select 1 from CommentReport r2
             where r2.comment.id = c.id and r2.createdAt >= :from
      ))
      and (:to is null or exists (
            select 1 from CommentReport r3
             where r3.comment.id = c.id and r3.createdAt < :to
      ))
""")
    Page<Comment> findAdminPending(
            @Param("reason") cotw.server.domain.comment.entity.ReportReason reason,
            @Param("from") java.time.LocalDateTime from,
            @Param("to") java.time.LocalDateTime to,
            Pageable pageable);

    @Query(value = """
    select c from Comment c
    where c.isPublic = false
      and c.deletedAt is null
      and (c.moderationDueAt is not null and c.moderationDueAt < CURRENT_TIMESTAMP)
      and (:reason is null or exists (
            select 1 from CommentReport r
             where r.comment.id = c.id and r.reason = :reason
      ))
      and (:from is null or exists (
            select 1 from CommentReport r2
             where r2.comment.id = c.id and r2.createdAt >= :from
      ))
      and (:to is null or exists (
            select 1 from CommentReport r3
             where r3.comment.id = c.id and r3.createdAt < :to
      ))
    order by c.moderationDueAt asc, c.id asc
""",
            countQuery = """
    select count(c) from Comment c
    where c.isPublic = false
      and c.deletedAt is null
      and (c.moderationDueAt is not null and c.moderationDueAt < CURRENT_TIMESTAMP)
      and (:reason is null or exists (
            select 1 from CommentReport r
             where r.comment.id = c.id and r.reason = :reason
      ))
      and (:from is null or exists (
            select 1 from CommentReport r2
             where r2.comment.id = c.id and r2.createdAt >= :from
      ))
      and (:to is null or exists (
            select 1 from CommentReport r3
             where r3.comment.id = c.id and r3.createdAt < :to
      ))
""")
    Page<Comment> findAdminExpired(
            @Param("reason") cotw.server.domain.comment.entity.ReportReason reason,
            @Param("from") java.time.LocalDateTime from,
            @Param("to") java.time.LocalDateTime to,
            Pageable pageable);

    @Query(value = """
    select c from Comment c
    where c.isPublic = false
      and (:reason is null or exists (
            select 1 from CommentReport r
             where r.comment.id = c.id and r.reason = :reason
      ))
      and (:from is null or exists (
            select 1 from CommentReport r2
             where r2.comment.id = c.id and r2.createdAt >= :from
      ))
      and (:to is null or exists (
            select 1 from CommentReport r3
             where r3.comment.id = c.id and r3.createdAt < :to
      ))
    order by c.updatedAt desc, c.id desc
""",
            countQuery = """
    select count(c) from Comment c
    where c.isPublic = false
      and (:reason is null or exists (
            select 1 from CommentReport r
             where r.comment.id = c.id and r.reason = :reason
      ))
      and (:from is null or exists (
            select 1 from CommentReport r2
             where r2.comment.id = c.id and r2.createdAt >= :from
      ))
      and (:to is null or exists (
            select 1 from CommentReport r3
             where r3.comment.id = c.id and r3.createdAt < :to
      ))
""")
    Page<Comment> findAdminHidden(
            @Param("reason") cotw.server.domain.comment.entity.ReportReason reason,
            @Param("from") java.time.LocalDateTime from,
            @Param("to") java.time.LocalDateTime to,
            Pageable pageable);

    @Query(value = """
    select c from Comment c
    where (c.reportCount > 0 or c.isPublic = false or c.moderationDueAt is not null)
      and (:reason is null or exists (
            select 1 from CommentReport r
             where r.comment.id = c.id and r.reason = :reason
      ))
      and (:from is null or exists (
            select 1 from CommentReport r2
             where r2.comment.id = c.id and r2.createdAt >= :from
      ))
      and (:to is null or exists (
            select 1 from CommentReport r3
             where r3.comment.id = c.id and r3.createdAt < :to
      ))
    order by c.updatedAt desc, c.id desc
""",
            countQuery = """
    select count(c) from Comment c
    where (c.reportCount > 0 or c.isPublic = false or c.moderationDueAt is not null)
      and (:reason is null or exists (
            select 1 from CommentReport r
             where r.comment.id = c.id and r.reason = :reason
      ))
      and (:from is null or exists (
            select 1 from CommentReport r2
             where r2.comment.id = c.id and r2.createdAt >= :from
      ))
      and (:to is null or exists (
            select 1 from CommentReport r3
             where r3.comment.id = c.id and r3.createdAt < :to
      ))
""")
    Page<Comment> findAdminAll(
            @Param("reason") cotw.server.domain.comment.entity.ReportReason reason,
            @Param("from") java.time.LocalDateTime from,
            @Param("to") java.time.LocalDateTime to,
            Pageable pageable);

    // ====== 대시보드 카운트 ======
    @Query("""
    select count(c) from Comment c
     where c.isPublic=false
       and c.deletedAt is null
       and c.moderationDueAt is not null
       and c.moderationDueAt >= CURRENT_TIMESTAMP
""")
    long countAdminPending();

    @Query("""
    select count(c) from Comment c
     where c.isPublic=false
       and c.deletedAt is null
       and c.moderationDueAt is not null
       and c.moderationDueAt < CURRENT_TIMESTAMP
""")
    long countAdminExpired();

}
