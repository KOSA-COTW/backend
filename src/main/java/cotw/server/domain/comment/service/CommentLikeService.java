package cotw.server.domain.comment.service;

import cotw.server.domain.comment.dto.request.LikeRequest;
import cotw.server.domain.comment.dto.response.LikeResponse;
import cotw.server.domain.comment.entity.Comment;
import cotw.server.domain.comment.entity.CommentLike;
import cotw.server.domain.comment.repository.CommentLikeRepository;
import cotw.server.domain.comment.repository.CommentRepository;
import cotw.server.domain.member.entity.Member;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
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
        if (!likeRepository.existsByCommentIdAndMemberId(commentId, req.memberId())) {
            likeRepository.save(CommentLike.builder()
                    .comment(c)
                    .member(em.getReference(Member.class, req.memberId()))
                    .build());
            c.setLikeCount(c.getLikeCount() + 1); // 비정규화 카운터 증가
        }
        return new LikeResponse(commentId, c.getLikeCount());
    }

    public LikeResponse unlike(Long commentId, Long memberId) {
        Comment c = get(commentId);
        likeRepository.findByCommentIdAndMemberId(commentId, memberId).ifPresent(l -> {
            likeRepository.delete(l);
            c.setLikeCount(Math.max(0, c.getLikeCount() - 1));
        });
        return new LikeResponse(commentId, c.getLikeCount());
    }

    private Comment get(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("comment not found: " + id));
    }
}
