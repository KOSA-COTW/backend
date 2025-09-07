
package cotw.server.domain.admin.dto.response;

public record AdminDashboardKpiResponse(
        long todayDonation,
        long totalDonation,
        long newUsers,
        long totalUsers,
        long pendingReports,
        long dueIn48h
) {}
