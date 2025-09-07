package cotw.server.domain.admin.controller;

import cotw.server.domain.admin.dto.response.AdminCategoryStatsResponse;
import cotw.server.domain.admin.dto.response.AdminDonationStatsResponse;
import cotw.server.domain.admin.service.AdminStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatsController {

    private final AdminStatsService service;

    /** 총 기부 금액 + 상위 10명 */
    @GetMapping("/donations")
    public ResponseEntity<AdminDonationStatsResponse> donationStats() {
        return ResponseEntity.ok(service.getDonationStats());
    }

    /** 카테고리별 기부금액 순위 */
    @GetMapping("/categories")
    public ResponseEntity<List<AdminCategoryStatsResponse>> categoryStats() {
        return ResponseEntity.ok(service.getCategoryStats());
    }
}
