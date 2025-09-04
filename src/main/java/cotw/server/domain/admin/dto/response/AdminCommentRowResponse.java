// src/main/java/cotw/server/domain/admin/dto/response/AdminCommentRowResponse.java
package cotw.server.domain.admin.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record AdminCommentRowResponse(
        Long id,
        String content,
        String authorEmail,
        String postTitle,
        boolean isPublic,
        int reportCount,
        String topReason,
        LocalDateTime createdAt,
        LocalDateTime moderationDueAt
) {}
