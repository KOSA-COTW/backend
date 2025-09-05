package cotw.server.domain.admin.controller;

import cotw.server.domain.admin.dto.response.*;
import cotw.server.domain.admin.service.AdminDonationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/donations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDonationController {

    private final AdminDonationService service;

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardStatsResponse> dashboard() {
        return ResponseEntity.ok(service.getDashboardStats());
    }

    @GetMapping("/top-donors")
    public ResponseEntity<List<AdminTopDonorDashboardResponse>> topDonors() {
        return ResponseEntity.ok(service.getTopDonors(10));
    }

    @GetMapping
    public ResponseEntity<AdminDonationListResponse> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(service.getDonations(page, size, search, status));
    }
}