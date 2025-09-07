package cotw.server.domain.admin.controller;

import cotw.server.domain.admin.dto.request.*;
import cotw.server.domain.admin.dto.response.*;
import cotw.server.domain.admin.service.AdminMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminMemberController {

    private final AdminMemberService service;

    @GetMapping
    public ResponseEntity<AdminMemberListResponse> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(service.search(keyword, role, status, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminMemberDetailResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id,
                                       @RequestBody AdminMemberProfileRequest req) {
        service.updateProfile(id, req);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/roles")
    public ResponseEntity<Void> updateRole(@PathVariable Long id,
                                           @RequestBody AdminMemberRoleRequest req) {
        service.updateRole(id, req);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/bulk-delete")
    public ResponseEntity<Void> bulkDelete(@RequestBody IdListRequest req) {
        service.bulkDelete(req.ids());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/bulk-status")
    public ResponseEntity<Void> bulkStatus(@RequestBody AdminMemberBulkStatusRequest req) {
        service.bulkUpdateStatus(req.ids(), req.status());
        return ResponseEntity.ok().build();
    }
}