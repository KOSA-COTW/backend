package cotw.server.domain.admin.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AdminReportBulkActionRequest(
        @NotNull Action action,
        @NotEmpty List<Long> commentIds
) {
    public enum Action { RESTORE, DELETE, RESET }
}