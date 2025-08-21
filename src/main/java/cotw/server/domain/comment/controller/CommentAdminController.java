package cotw.server.domain.comment.controller;

import cotw.server.domain.comment.service.CommentReportService;
import cotw.server.domain.comment.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/comments")
public class CommentAdminController {

    private final CommentReportService reportService;
    private final CommentService commentService;

    /** (관리자) 임시 숨김 해제(복원) */
    @PostMapping("/{commentId}/restore")
    public ResponseEntity<Void> restoreHidden(@PathVariable Long commentId) {
        reportService.adminRestoreHidden(commentId);
        return ResponseEntity.noContent().build();
    }

    /** (관리자) 신고 초기화 */
    @PostMapping("/{commentId}/reset-reports")
    public ResponseEntity<Void> resetReports(@PathVariable Long commentId) {
        reportService.adminResetReports(commentId);
        return ResponseEntity.noContent().build();
    }

    /** (관리자) 댓글 삭제(소프트) */
    @PostMapping("/{commentId}/delete")
    public ResponseEntity<Void> adminDelete(@PathVariable Long commentId) {
        commentService.adminDelete(commentId);
        return ResponseEntity.noContent().build();
    }
}
