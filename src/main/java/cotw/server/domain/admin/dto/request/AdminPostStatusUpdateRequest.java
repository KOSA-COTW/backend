package cotw.server.domain.admin.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;


public record AdminPostStatusUpdateRequest(
        @NotNull Long postId,
        String status,
        Boolean isPublic,
        LocalDate deadline
) {}
