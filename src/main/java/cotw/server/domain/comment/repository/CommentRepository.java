// src/main/java/cotw/server/domain/comment/repository/CommentRepository.java
package cotw.server.domain.comment.repository;

import cotw.server.domain.board.entity.Comment;
import cotw.server.domain.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
public interface CommentRepository extends JpaRepository<Comment, Long>, JpaSpecificationExecutor<Comment> {




    // ===== 사용자: 최신순 =====
    @Query(value = """
        select c
        from Comment c
        where c.post.id = :postId
          and c.deletedAt is null
          and (
                :admin = true
             or (
                   c.isPublic = true
                or ( :viewerId is not null and c.member.id = :viewerId )
             )
          )
        order by c.createdAt desc, c.id desc
      """,
            countQuery = """
        select count(c)
        from Comment c
        where c.post.id = :postId
          and c.deletedAt is null
          and (
                :admin = true
             or (
                   c.isPublic = true
                or ( :viewerId is not null and c.member.id = :viewerId )
             )
          )
      """
    )
    Page<Comment> findLatestVisible(@Param("postId") Long postId,
                                    @Param("viewerId") Long viewerId,
                                    @Param("admin") boolean admin,
                                    Pageable pageable);

    // ===== 사용자: 공감순 =====
    @Query(value = """
        select c
        from Comment c
        where c.post.id = :postId
          and c.deletedAt is null
          and (
                :admin = true
             or (
                   c.isPublic = true
                or ( :viewerId is not null and c.member.id = :viewerId )
             )
          )
        order by c.likeCount desc, c.id desc
      """,
            countQuery = """
        select count(c)
        from Comment c
        where c.post.id = :postId
          and c.deletedAt is null
          and (
                :admin = true
             or (
                   c.isPublic = true
                or ( :viewerId is not null and c.member.id = :viewerId )
             )
          )
      """
    )
    Page<Comment> findLikeVisible(@Param("postId") Long postId,
                                  @Param("viewerId") Long viewerId,
                                  @Param("admin") boolean admin,
                                  Pageable pageable);

    // ===== 비정규화 카운터: 원자적 증가/감소 =====
    @Modifying(flushAutomatically = true)
    @Query("update Comment c set c.likeCount = c.likeCount + 1 where c.id = :commentId")
    int incrementLikeCount(@Param("commentId") Long commentId);

    @Modifying(flushAutomatically = true)
    @Query("""
        update Comment c
           set c.likeCount = case when c.likeCount > 0 then c.likeCount - 1 else 0 end
         where c.id = :commentId
    """)
    int decrementLikeCount(@Param("commentId") Long commentId);



    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Comment c
           set c.member = :deletedUser
         where c.member.id in :memberIds
    """)
    int anonymizeAuthorByMemberIds(@Param("memberIds") List<Long> memberIds,
                                   @Param("deletedUser") Member deletedUser);

    // ====== 관리자 전용 조회 ======

    // PENDING : 숨김 & 삭제 아님 & 검토기한 >= now
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

    // EXPIRED : 숨김 & 삭제 아님 & 검토기한 < now
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

    // HIDDEN : 숨김 전체 (삭제 제외)
    @Query(value = """
    select c from Comment c
    where c.isPublic = false
      and c.deletedAt is null
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
      and c.deletedAt is null
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

    // ALL : 신고/숨김/검토대상 (삭제 제외)
    @Query(value = """
    select c from Comment c
    where (c.reportCount > 0 or c.isPublic = false or c.moderationDueAt is not null)
      and c.deletedAt is null
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
      and c.deletedAt is null
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

    // ===== 관리자 Dynamic =====
    @EntityGraph(attributePaths = {"member", "post"})
    @Query(value = """
        select c
          from Comment c
          left join c.member m
          left join c.post p
         where c.deletedAt is null
           and ( :kw is null
                 or lower(c.content) like :kw
                 or (m.email is not null and lower(m.email) like :kw)
                 or (p.title is not null and lower(p.title) like :kw)
               )
           and ( :reason is null
                 or exists (select 1 from CommentReport r where r.comment.id = c.id and r.reason = :reason)
               )
           and ( :from is null or c.createdAt >= :from )
           and ( :to   is null or c.createdAt <= :to )
           and (
                  :status = 'ALL'
               or (:status = 'HIDDEN'  and c.isPublic = false)
               or (:status = 'PENDING' and c.moderationDueAt is not null and c.moderationDueAt >= CURRENT_TIMESTAMP)
               or (:status = 'EXPIRED' and c.moderationDueAt is not null and c.moderationDueAt <  CURRENT_TIMESTAMP)
               )
           and ( :reportedOnly is null
                 or :reportedOnly = false
                 or ( :reportedOnly = true and c.reportCount > 0 )
               )
        """,
            countQuery = """
        select count(c)
          from Comment c
          left join c.member m
          left join c.post p
         where c.deletedAt is null
           and ( :kw is null
                 or lower(c.content) like :kw
                 or (m.email is not null and lower(m.email) like :kw)
                 or (p.title is not null and lower(p.title) like :kw)
               )
           and ( :reason is null
                 or exists (select 1 from CommentReport r where r.comment.id = c.id and r.reason = :reason)
               )
           and ( :from is null or c.createdAt >= :from )
           and ( :to   is null or c.createdAt <= :to )
           and (
                  :status = 'ALL'
               or (:status = 'HIDDEN'  and c.isPublic = false)
               or (:status = 'PENDING' and c.moderationDueAt is not null and c.moderationDueAt >= CURRENT_TIMESTAMP)
               or (:status = 'EXPIRED' and c.moderationDueAt is not null and c.moderationDueAt <  CURRENT_TIMESTAMP)
               )
           and ( :reportedOnly is null
                 or :reportedOnly = false
                 or ( :reportedOnly = true and c.reportCount > 0 )
               )
        """)
    Page<Comment> findAdminDynamic(
            @Param("kw") String kw,
            @Param("status") String status,
            @Param("reason") cotw.server.domain.comment.entity.ReportReason reason,
            @Param("from") java.time.LocalDateTime from,
            @Param("to") java.time.LocalDateTime to,
            @Param("reportedOnly") Boolean reportedOnly,
            Pageable pageable
    );

    // ===== 대시보드 카운트 =====
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

    @Query("""
        select count(c) from Comment c
         where c.isPublic = false
           and c.deletedAt is null
    """)
    long countAdminHiddenAll();

    @Query("""
        select count(c) from Comment c
         where c.reportCount > 0
           and c.deletedAt is null
    """)
    long countAdminReported();
}
