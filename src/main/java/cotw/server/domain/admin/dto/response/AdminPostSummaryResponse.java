package cotw.server.domain.admin.dto.response;

import java.time.LocalDateTime;


public record AdminPostSummaryResponse(
        Long postId,
        String title,
        String status,          // Post.getStatus() => "ONGOING" / "COMPLETED"
        boolean isPublic,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
