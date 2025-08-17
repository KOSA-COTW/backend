package cotw.server.domain.comment.service;

import cotw.server.domain.board.domain.Post;
import cotw.server.domain.comment.dto.request.CreateCommentRequest;
import cotw.server.domain.comment.dto.request.UpdateCommentRequest;
import cotw.server.domain.comment.dto.response.CommentResponse;
import cotw.server.domain.comment.entity.Comment;

import cotw.server.domain.comment.repository.CommentRepository;
import cotw.server.domain.member.entity.Member;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final EntityManager em;

    // 작성
    public CommentResponse create(CreateCommentRequest req) {
        Post postRef = em.getReference(Post.class, req.postId());
        Member memRef = em.getReference(Member.class, req.memberId());
        Comment c = commentRepository.save(Comment.builder()
                .post(postRef).member(memRef).content(req.content())
                .build());
        return toRes(c);
    }

    // 수정(본인만)
    public CommentResponse update(Long commentId, Long requesterId, UpdateCommentRequest req) {
        Comment c = get(commentId);
        requireOwner(c, requesterId);
        if (c.getDeletedAt() != null) throw new IllegalStateException("삭제된 댓글입니다.");
        c.setContent(req.content());
        return toRes(c);
    }

    // 삭제(소프트): 본인 or 관리자
    public void delete(Long commentId, Long requesterId, boolean admin) {
        Comment c = get(commentId);
        if (!admin) requireOwner(c, requesterId);
        c.setDeletedAt(LocalDateTime.now());
        c.setPublic(false);
    }

    // 목록: 최신순
    @Transactional(readOnly = true)
    public Page<CommentResponse> listLatest(Long postId, Long viewerId, boolean admin, Pageable pageable) {
        return commentRepository.findLatestVisible(postId, viewerId, admin, pageable)
                .map(this::toRes);
    }

    // 목록: 좋아요순
    @Transactional(readOnly = true)
    public Page<CommentResponse> listByLike(Long postId, Long viewerId, boolean admin, Pageable pageable) {
        return commentRepository.findLikeVisible(postId, viewerId, admin, pageable)
                .map(this::toRes);
    }

    // 관리자 복원/삭제
    public void adminRestore(Long commentId) {
        Comment c = get(commentId);
        c.setPublic(true);
        c.setModerationDueAt(null);
        c.setDeletedAt(null);
    }
    public void adminDelete(Long commentId) {
        Comment c = get(commentId);
        c.setDeletedAt(LocalDateTime.now());
        c.setPublic(false);
    }

    // 내부 유틸
    private Comment get(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("comment not found: " + id));
    }
    private void requireOwner(Comment c, Long requesterId) {
        if (!Objects.equals(c.getMember().getId(), requesterId))
            throw new AccessDeniedException("권한이 없습니다.");
    }
    private CommentResponse toRes(Comment c) {
        return new CommentResponse(
                c.getId(), c.getPost().getId(), c.getMember().getId(),
                c.getContent(), c.getLikeCount(), c.getReportCount(), c.isPublic(),
                c.getCreatedAt(), c.getModerationDueAt()
        );
    }
}
