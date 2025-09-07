package cotw.server.domain.admin.dto.response;

import java.util.List;

public record AdminDashboardResponse(
        AdminDashboardKpiResponse kpi,
        List<AdminRecentPaymentResponse> recentPayments,
        List<AdminRecentReportResponse> recentReports,
        AdminDailyDonationResponse dailyDonation
) {}