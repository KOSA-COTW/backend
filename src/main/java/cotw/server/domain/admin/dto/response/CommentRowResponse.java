package cotw.server.domain.admin.dto.response;

import java.time.LocalDateTime;

public record CommentRowResponse(
        Long id,
        String content,
        String authorEmail,
        String postTitle,
        boolean isPublic,
        Integer reportCount,
        String topReason,
        LocalDateTime createdAt,
        LocalDateTime moderationDueAt
) { }
