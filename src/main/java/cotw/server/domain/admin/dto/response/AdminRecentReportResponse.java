package cotw.server.domain.admin.dto.response;

import java.time.LocalDateTime;

public record AdminRecentReportResponse(
        String reportedMemberEmail,
        String reason,
        long count,
        LocalDateTime at
) {}