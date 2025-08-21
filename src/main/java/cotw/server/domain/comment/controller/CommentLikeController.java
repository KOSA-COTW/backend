package cotw.server.domain.comment.controller;

import cotw.server.domain.comment.dto.request.LikeRequest;
import cotw.server.domain.comment.dto.response.LikeResponse;
import cotw.server.domain.comment.service.CommentLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;


@RestController
@RequestMapping("/api/comments/{commentId}/likes")
@RequiredArgsConstructor
public class CommentLikeController {

    private final CommentLikeService likeService;

    /** 좋아요 (JWT에서 memberId 사용) */
    @PostMapping
    public ResponseEntity<LikeResponse> like(@PathVariable Long commentId,
                                             @AuthenticationPrincipal(expression = "id") Long memberId) {
        return ResponseEntity.ok(likeService.like(commentId, new LikeRequest(memberId)));
    }

    /** 좋아요 취소 (JWT에서 memberId 사용) */
    @DeleteMapping
    public ResponseEntity<LikeResponse> unlike(@PathVariable Long commentId,
                                               @AuthenticationPrincipal(expression = "id") Long memberId) {
        return ResponseEntity.ok(likeService.unlike(commentId, memberId));
    }
}
