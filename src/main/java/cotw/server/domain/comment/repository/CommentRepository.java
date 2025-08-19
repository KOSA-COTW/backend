package cotw.server.domain.comment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import cotw.server.domain.board.entity.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query(
            value = """
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

    @Query(
            value = """
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
}
