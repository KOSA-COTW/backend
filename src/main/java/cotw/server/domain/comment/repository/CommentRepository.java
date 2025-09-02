// src/main/java/cotw/server/domain/comment/repository/CommentRepository.java
package cotw.server.domain.comment.repository;

import cotw.server.domain.board.entity.Comment;
import cotw.server.domain.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

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


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Comment c
           set c.member = :deletedUser
         where c.member.id in :memberIds
    """)
    int anonymizeAuthorByMemberIds(@Param("memberIds") List<Long> memberIds,
                                   @Param("deletedUser") Member deletedUser);
}
