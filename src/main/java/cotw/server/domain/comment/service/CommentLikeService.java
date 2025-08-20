// src/main/java/cotw/server/domain/comment/service/CommentLikeService.java
package cotw.server.domain.comment.service;

import cotw.server.domain.board.entity.Comment;
import cotw.server.domain.comment.dto.request.LikeRequest;
import cotw.server.domain.comment.dto.response.LikeResponse;
import cotw.server.domain.comment.entity.CommentLike;
import cotw.server.domain.comment.repository.CommentLikeRepository;
import cotw.server.domain.comment.repository.CommentRepository;
import cotw.server.domain.member.entity.Member;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentLikeService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository likeRepository;
    private final EntityManager em;

    public LikeResponse like(Long commentId, LikeRequest req) {
        Comment c = get(commentId);

        try {
            // UNIQUE(comment_id, member_id) 제약으로 중복 좋아요는 여기서 예외 발생
            likeRepository.save(CommentLike.builder()
                    .comment(c)
                    .member(em.getReference(Member.class, req.memberId()))
                    .build());

            // 실제 삽입된 경우에만 원자적 증가
            commentRepository.incrementLikeCount(commentId);

            // 최신 카운트를 정확히 주고 싶으면 flush+refresh
            em.flush();
            em.refresh(c);

            return new LikeResponse(commentId, c.getLikeCount(), true);

        } catch (DataIntegrityViolationException e) {
            // 이미 좋아요 상태(멱등)
            // 최신값 보장을 위해 refresh 시도(필수는 아님)
            em.flush();
            em.refresh(c);
            return new LikeResponse(commentId, c.getLikeCount(), true);
        }
    }

    public LikeResponse unlike(Long commentId, Long memberId) {
        Comment c = get(commentId);

        long deleted = likeRepository.deleteByCommentIdAndMemberId(commentId, memberId);
        if (deleted > 0) {
            commentRepository.decrementLikeCount(commentId);
            em.flush();
            em.refresh(c);
            return new LikeResponse(commentId, c.getLikeCount(), false);
        } else {
            // 이미 좋아요가 없는 상태(멱등)
            em.flush();
            em.refresh(c);
            return new LikeResponse(commentId, c.getLikeCount(), false);
        }
    }

    private Comment get(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("comment not found: " + id));
    }
}
