package cotw.server.domain.comment.service;

import cotw.server.domain.board.entity.Comment;
import cotw.server.domain.comment.dto.request.LikeRequest;
import cotw.server.domain.comment.dto.response.LikeResponse;
import cotw.server.domain.comment.entity.CommentLike;
import cotw.server.domain.comment.repository.CommentLikeRepository;
import cotw.server.domain.comment.repository.CommentRepository;
import cotw.server.domain.member.entity.Member;
import jakarta.persistence.EntityManager;
import cotw.server.domain.comment.exception.CommentException;
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
        // 관리 상태 보장 (없으면 404)
        Comment c = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentException("comment not found: " + commentId, "COMMENT_NOT_FOUND"));

        try {
            // UNIQUE(comment_id, member_id) 제약으로 중복 좋아요 시 예외 발생
            likeRepository.save(CommentLike.builder()
                    .comment(c) // 이미 관리 상태
                    .member(em.getReference(Member.class, req.memberId()))
                    .build());

            // 실제 삽입된 경우만 증가
            commentRepository.incrementLikeCount(commentId);

            // 최신 카운트 보장
            em.flush();
            em.refresh(c);

            return new LikeResponse(commentId, c.getLikeCount(), true);

        } catch (DataIntegrityViolationException e) {
            // 이미 좋아요(멱등)
            em.flush();
            em.refresh(c);
            return new LikeResponse(commentId, c.getLikeCount(), true);
        }
    }

    public LikeResponse unlike(Long commentId, Long memberId) {
        // 관리 상태 보장 (없으면 404)
        Comment c = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentException("comment not found: " + commentId, "COMMENT_NOT_FOUND"));

        long deleted = likeRepository.deleteByCommentIdAndMemberId(commentId, memberId);
        if (deleted > 0) {
            commentRepository.decrementLikeCount(commentId);
        }

        em.flush();
        em.refresh(c);
        return new LikeResponse(commentId, c.getLikeCount(), false);
    }
}