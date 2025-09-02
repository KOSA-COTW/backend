package cotw.server.domain.comment.controller;

import cotw.server.domain.comment.dto.request.CreateCommentRequest;
import cotw.server.domain.comment.dto.request.UpdateCommentRequest;
import cotw.server.domain.comment.dto.response.CommentResponse;
import cotw.server.domain.comment.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /** 댓글 작성 - JWT의 memberId 사용 */
    @PostMapping
    public ResponseEntity<CommentResponse> create(
            @AuthenticationPrincipal(expression = "id") Long memberId,
            @Valid @RequestBody CreateCommentRequest req
    ) {
        return ResponseEntity.ok(commentService.create(memberId, req));
    }

    /** 댓글 목록: 최신/좋아요 정렬 */
    @GetMapping
    public ResponseEntity<Page<CommentResponse>> list(
            @RequestParam Long postId,
            @AuthenticationPrincipal cotw.server.common.jwt.CustomUserDetails principal,
            Authentication authentication,
            @RequestParam(defaultValue = "LATEST") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long viewerId = (principal != null) ? principal.getId() : null;
        boolean admin = isAdmin(authentication);

        Page<CommentResponse> pageResult;
        if ("LIKE".equalsIgnoreCase(sort)) {
            // 좋아요순 (likeCount desc, id desc)
            Pageable realPageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("likeCount"), Sort.Order.desc("id")));
            pageResult = commentService.listByLike(postId, viewerId, admin, realPageable);
        } else {
            // 최신순 (createdAt desc, id desc)
            Pageable realPageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
            pageResult = commentService.listLatest(postId, viewerId, admin, realPageable);
        }

        return ResponseEntity.ok(pageResult);
    }

    /** 댓글 수정(본인만) */
    @PatchMapping("/{commentId}")
    public ResponseEntity<CommentResponse> update(
            @PathVariable Long commentId,
            @AuthenticationPrincipal(expression = "id") Long requesterId,
            @Valid @RequestBody UpdateCommentRequest req
    ) {
        return ResponseEntity.ok(commentService.update(commentId, requesterId, req));
    }

    /** 댓글 삭제(소프트) - 본인 or 관리자 */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long commentId,
            @AuthenticationPrincipal(expression = "id") Long requesterId,
            Authentication authentication
    ) {
        boolean admin = isAdmin(authentication);
        commentService.delete(commentId, requesterId, admin);
        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .anyMatch(auth -> "ADMIN".equals(auth) || "ROLE_ADMIN".equals(auth));
    }
}
