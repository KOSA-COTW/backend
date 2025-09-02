// src/main/java/cotw/server/domain/comment/repository/CommentLikeRepository.java
package cotw.server.domain.comment.repository;



import cotw.server.domain.comment.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    boolean existsByCommentIdAndMemberId(Long commentId, Long memberId);
    Optional<CommentLike> findByCommentIdAndMemberId(Long commentId, Long memberId);
    long deleteByCommentIdAndMemberId(Long commentId, Long memberId); // ★ 추가

    void deleteByMemberIdIn(Collection<Long> memberIds);
}
