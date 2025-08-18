package cotw.server.domain.comment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import cotw.server.domain.board.entity.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 최신순: 일반유저는 공개+미삭제만, 작성자/관리자면 숨김도 보이게
    @Query("""
       select c from Comment c
       where c.post.id = :postId
         and (c.deletedAt is null)
         and (c.isPublic = true or c.member.id = :viewerId or :admin = true)
       order by c.createdAt desc
    """)
    Page<Comment> findLatestVisible(@Param("postId") Long postId,
                                    @Param("viewerId") Long viewerId,
                                    @Param("admin") boolean admin,
                                    Pageable pageable);

    // 좋아요순
    @Query("""
       select c from Comment c
       where c.post.id = :postId
         and (c.deletedAt is null)
         and (c.isPublic = true or c.member.id = :viewerId or :admin = true)
       order by c.likeCount desc, c.id desc
    """)
    Page<Comment> findLikeVisible(@Param("postId") Long postId,
                                  @Param("viewerId") Long viewerId,
                                  @Param("admin") boolean admin,
                                  Pageable pageable);
}