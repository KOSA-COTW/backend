package cotw.server.domain.comment.dto.response;

import java.time.LocalDateTime;

public record CommentResponse(
        Long id,
        Long postId,
        Long memberId,
        String content,
        int likeCount,
        int reportCount,
        boolean isPublic,
        LocalDateTime createdAt,
        LocalDateTime moderationDueAt,
        boolean liked,
        String authorEmail
) {}
