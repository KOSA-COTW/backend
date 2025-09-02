package cotw.server.domain.admin.dto.request;

import cotw.server.domain.board.entity.PostVisibility;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;


public record AdminPostStatusUpdateRequest(
        @NotNull Long postId,
        String status,
        PostVisibility visibilityStatus,
        LocalDate deadline
) {}