package cotw.server.domain.admin.controller;

import cotw.server.domain.admin.dto.request.*;
import cotw.server.domain.admin.dto.response.*;
import cotw.server.domain.admin.service.AdminCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/comments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCommentController {

    private final AdminCommentService service;

    // ✅ 목록 조회: GET + @ModelAttribute 바인딩
    @GetMapping
    public Page<CommentRowResponse> list(
            @ModelAttribute CommentListRequest req,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return service.list(req, PageRequest.of(page, size));
    }

    @GetMapping("/{id}/reports")
    public java.util.List<ReportLogResponse> reportLogs(@PathVariable Long id) {
        return service.reportLogs(id);
    }

    @PutMapping("/{id}/visibility")
    public void setVisibility(@PathVariable Long id, @RequestParam boolean visible) {
        service.setVisibility(id, visible);
    }

    @PostMapping("/{id}/reset-reports")
    public void resetReports(@PathVariable Long id) {
        service.resetReports(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PutMapping("/{id}/memo")
    public void saveMemo(@PathVariable Long id, @RequestBody CommentMemoRequest req) {
        service.saveMemo(id, req.memo());
    }

    // ---- bulk
    @PostMapping("/bulk/visibility")
    public void bulkVisibility(@RequestBody BulkVisibilityRequest req) {
        service.bulkVisibility(req.ids(), req.visible());
    }

    @PostMapping("/bulk/reset-reports")
    public void bulkReset(@RequestBody IdsRequest req) {
        service.bulkResetReports(req.ids());
    }

    @PostMapping("/bulk/delete")
    public void bulkDelete(@RequestBody IdsRequest req) {
        service.bulkDelete(req.ids());
    }
}