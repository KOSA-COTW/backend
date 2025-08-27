package cotw.server.domain.comment.dto.response;

import cotw.server.domain.comment.entity.ReportReason;

import java.time.LocalDateTime;

public record ReportResponse(
        Long commentId,
        Long reporterId,
        ReportReason reason,
        int totalReportCount,
        boolean hidden,               // 3회 도달로 숨김 상태인지
        LocalDateTime moderationDue   // 숨김이면 48h 마감 시각
) {}
