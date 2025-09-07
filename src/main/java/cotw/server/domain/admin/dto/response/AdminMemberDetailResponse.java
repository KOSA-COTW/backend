package cotw.server.domain.admin.dto.response;

import cotw.server.domain.member.entity.AccountStatus;
import java.util.List;

public record AdminMemberDetailResponse(
        long id,
        String name,
        String email,
        AccountStatus status,
        List<String> roles
) {}