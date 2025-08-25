package cotw.server.domain.admin.dto.response;

public record AdminDashboardSummaryResponse(
        long todayReports,
        long pendingHidden,   // 숨김 상태(기한 미경과)
        long expiredHidden,   // 숨김 + 만료(48h 경과)
        long totalComments
) {}