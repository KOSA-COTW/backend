package cotw.server.domain.admin.dto.response;

public record AdminDashboardStatsResponse(
        long totalAmount,
        long totalDonations,
        long totalMembers
) {}