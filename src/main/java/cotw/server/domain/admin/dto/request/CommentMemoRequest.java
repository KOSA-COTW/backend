package cotw.server.domain.admin.dto.request;

import jakarta.validation.constraints.Size;

public record CommentMemoRequest(
        @Size(max = 500) String memo
) { }