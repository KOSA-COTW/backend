package cotw.server.domain.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminPostCountResponseDTO {
    private long totalCount;
    private long privateCount;
    private long pendingCount;
    private long approvedCount;
    private long rejectedCount;
}