package cotw.server.domain.comment.dto.request;

import jakarta.validation.constraints.NotNull;

public record LikeRequest(@NotNull Long memberId) {}
