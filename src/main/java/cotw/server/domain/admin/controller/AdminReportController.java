package cotw.server.domain.admin.controller;

import cotw.server.domain.admin.dto.request.AdminReportBulkActionRequest;
import cotw.server.domain.admin.dto.response.AdminReportDetailResponse;
import cotw.server.domain.admin.dto.response.AdminReportItemResponse;
import cotw.server.domain.admin.service.AdminReportService;
import cotw.server.domain.comment.entity.ReportReason;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

    private final AdminReportService service;

    /**
     * 신고 댓글 목록 조회
     *
     * status: ALL | PENDING | EXPIRED | HIDDEN (기본: ALL)
     * reason: (선택) ReportReason 로 필터
     * from/to: (선택) 최근 신고 발생 시각 범위
     */
    @GetMapping
    public ResponseEntity<Page<AdminReportItemResponse>> list(
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(required = false) ReportReason reason,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(service.list(status, reason, from, to, pageable));
    }

    /** 상세 조회 (사유 집계 포함) */
    @GetMapping("/{commentId}")
    public ResponseEntity<AdminReportDetailResponse> detail(@PathVariable Long commentId) {
        return ResponseEntity.ok(service.detail(commentId));
    }

    /** 개별 복원: 신고수 0, 공개 전환, 삭제/만료 플래그 초기화 */
    @PostMapping("/{commentId}/restore")
    public ResponseEntity<Void> restore(@PathVariable Long commentId) {
        service.restore(commentId);
        return ResponseEntity.noContent().build();
    }

    /** 개별 삭제(소프트): isPublic=false, deletedAt=now */
    @PostMapping("/{commentId}/delete")
    public ResponseEntity<Void> delete(@PathVariable Long commentId) {
        service.softDelete(commentId);
        return ResponseEntity.noContent().build();
    }

    /** 신고수만 초기화(로그는 유지), 공개 전환 */
    @PostMapping("/{commentId}/reset")
    public ResponseEntity<Void> reset(@PathVariable Long commentId) {
        service.reset(commentId);
        return ResponseEntity.noContent().build();
    }

    /** 일괄 처리 */
    @PostMapping("/bulk")
    public ResponseEntity<Void> bulk(@Valid @RequestBody AdminReportBulkActionRequest req) {
        service.bulk(req);
        return ResponseEntity.noContent().build();
    }
}