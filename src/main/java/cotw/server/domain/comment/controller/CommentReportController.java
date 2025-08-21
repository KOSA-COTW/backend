package cotw.server.domain.comment.controller;

import cotw.server.domain.comment.dto.request.ReportRequest;
import cotw.server.domain.comment.dto.response.ReportResponse;
import cotw.server.domain.comment.entity.ReportReason;
import cotw.server.domain.comment.service.CommentReportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentReportController {

    private final CommentReportService reportService;

    /** 신고(사유 필수, 하루 3회 제한, 3회 도달 시 임시 숨김) */
    @PostMapping("/{commentId}/reports")
    public ResponseEntity<ReportResponse> report(@PathVariable Long commentId,
                                                 @AuthenticationPrincipal(expression = "id") Long memberId,
                                                 @Valid @RequestBody ReportReasonBody body) {
        ReportRequest req = new ReportRequest(memberId, body.reason());
        return ResponseEntity.ok(reportService.report(commentId, req));
    }

    /** 요청 바디: 신고 사유만 받음 */
    public record ReportReasonBody(@NotNull ReportReason reason) {}
}
