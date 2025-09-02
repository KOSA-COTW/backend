package cotw.server.domain.admin.dto.response;

import cotw.server.domain.comment.entity.ReportReason;
import java.time.LocalDateTime;
import java.util.Map;

public record AdminReportDetailResponse(
        Long commentId,
        Long postId,
        Long memberId,
        String content,
        int reportCount,
        boolean hidden,
        LocalDateTime moderationDueAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime lastReportedAt,
        Map<ReportReason, Long> reasonBreakdown
) {}