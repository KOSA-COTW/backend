package cotw.server.domain.comment.controller;

import cotw.server.domain.comment.dto.request.ReportRequest;
import cotw.server.domain.comment.dto.response.ReportResponse;
import cotw.server.domain.comment.service.CommentReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 댓글 신고 API
 */
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentReportController {

    private final CommentReportService reportService;

    /**
     * 댓글 신고 생성
     * - 인증 사용자만 접근(@PreAuthorize)
     * - 요청 바디는 ReportRequest(사유/상세) 검증 적용(@Valid)
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{commentId}/reports")
    public ResponseEntity<ReportResponse> report(@PathVariable Long commentId,
                                                 @AuthenticationPrincipal(expression = "id") Long memberId,
                                                 @Valid @RequestBody ReportRequest req) {
        ReportResponse res = reportService.report(memberId, commentId, req);
        return ResponseEntity.ok(res);
    }
}
