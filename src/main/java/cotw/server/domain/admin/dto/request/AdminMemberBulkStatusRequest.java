package cotw.server.domain.admin.dto.request;

import cotw.server.domain.member.entity.AccountStatus;
import java.util.List;

public record AdminMemberBulkStatusRequest(
        List<Long> ids,
        AccountStatus status
) {}