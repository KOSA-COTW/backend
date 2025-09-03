package cotw.server.domain.admin.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record BulkVisibilityRequest(
        @NotNull List<Long> ids,
        boolean visible
) { }