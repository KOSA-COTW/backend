package cotw.server.domain.admin.controller;

import cotw.server.domain.admin.dto.request.AdminCommentSearchRequest;
import cotw.server.domain.admin.dto.request.IdListRequest;
import cotw.server.domain.admin.dto.request.VisibilityRequest;
import cotw.server.domain.admin.dto.response.AdminCommentRowResponse;
import cotw.server.domain.admin.dto.response.AdminReportLogResponse;
import cotw.server.domain.admin.service.AdminReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/comments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

    private final AdminReportService svc;

    // 목록
    @GetMapping
    public ResponseEntity<Page<AdminCommentRowResponse>> list(AdminCommentSearchRequest q) {
        return ResponseEntity.ok(svc.list(q));
    }

    // ✅ 신고 로그 (Service 통해 조회)
    @GetMapping("/{id}/report-logs")
    public ResponseEntity<List<AdminReportLogResponse>> logs(@PathVariable Long id) {
        return ResponseEntity.ok(svc.reportLogs(id));
    }

    // 공개/비공개
    @PatchMapping("/{id}/visibility")
    public ResponseEntity<Void> visibility(@PathVariable Long id, @RequestBody VisibilityRequest req) {
        svc.setVisibility(id, req.visible());
        return ResponseEntity.noContent().build();
    }

    // 신고수 초기화
    @PostMapping("/{id}/reset-reports")
    public ResponseEntity<Void> reset(@PathVariable Long id) {
        svc.resetReports(id);
        return ResponseEntity.noContent().build();
    }

    // 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        svc.deleteOne(id);
        return ResponseEntity.noContent().build();
    }

    // 일괄 공개/비공개
    @PostMapping("/bulk/visibility")
    public ResponseEntity<Void> bulkVisibility(@RequestBody IdListRequest body,
                                               @RequestParam boolean visible) {
        svc.bulkVisibility(body.ids(), visible);
        return ResponseEntity.noContent().build();
    }

    // 일괄 신고 초기화
    @PostMapping("/bulk/reset-reports")
    public ResponseEntity<Void> bulkReset(@RequestBody IdListRequest body) {
        svc.bulkResetReports(body.ids());
        return ResponseEntity.noContent().build();
    }

    // 일괄 삭제
    @PostMapping("/bulk/delete")
    public ResponseEntity<Void> bulkDelete(@RequestBody IdListRequest body) {
        svc.bulkDelete(body.ids());
        return ResponseEntity.noContent().build();
    }
}
