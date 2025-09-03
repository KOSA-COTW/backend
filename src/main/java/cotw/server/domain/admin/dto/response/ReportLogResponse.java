package cotw.server.domain.admin.dto.response;

import java.time.LocalDateTime;

public record ReportLogResponse(
        String reason,
        String reporter,
        LocalDateTime createdAt
) { }