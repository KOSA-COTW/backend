package cotw.server.domain.admin.dto.response;

import java.time.LocalDateTime;

public record AdminReportItemResponse(
        Long commentId,
        Long postId,
        Long memberId,
        String contentPreview,
        int reportCount,
        boolean hidden,         // !isPublic
        LocalDateTime moderationDueAt,
        LocalDateTime createdAt,
        LocalDateTime lastReportedAt
) {}
