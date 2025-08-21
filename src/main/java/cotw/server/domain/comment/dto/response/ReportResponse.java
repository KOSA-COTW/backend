package cotw.server.domain.comment.dto.response;

import java.time.LocalDateTime;

public record ReportResponse(
        Long commentId,
        int reportCount,
        boolean isPublic,
        LocalDateTime moderationDueAt
) {}
