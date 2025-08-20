package cotw.server.domain.comment.dto.request;

import cotw.server.domain.comment.entity.ReportReason;
import jakarta.validation.constraints.NotNull;

public record ReportRequest(
        @NotNull Long memberId,
        @NotNull ReportReason reason   // 스팸/욕설/부적절 등 (필수)
) {}
