package cotw.server.domain.admin.controller;

import cotw.server.domain.admin.dto.request.AdminPostStatusUpdateRequest;
import cotw.server.domain.admin.dto.response.AdminPostDetailResponse;
import cotw.server.domain.admin.dto.response.AdminPostSummaryResponse;
import cotw.server.domain.admin.service.AdminPostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/posts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPostController {

    private final AdminPostService service;

    /** 모든 기부글 조회 */
    @GetMapping
    public ResponseEntity<Page<AdminPostSummaryResponse>> list(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.list(pageable));
    }

    /** 단건 상세 */
    @GetMapping("/{postId}")
    public ResponseEntity<AdminPostDetailResponse> detail(@PathVariable Long postId) {
        return ResponseEntity.ok(service.detail(postId));
    }

    /** 상태/공개여부/마감일 변경 */
    @PatchMapping("/status")
    public ResponseEntity<Void> updateStatus(@Valid @RequestBody AdminPostStatusUpdateRequest req) {
        service.updateStatus(req);
        return ResponseEntity.noContent().build();
    }

    /** 공개/비공개 토글 */
    @PostMapping("/{postId}/toggle-visibility")
    public ResponseEntity<Void> toggleVisibility(@PathVariable Long postId) {
        service.toggleVisibility(postId);
        return ResponseEntity.noContent().build();
    }
}
