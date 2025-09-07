package cotw.server.domain.admin.dto.request;

import cotw.server.domain.member.entity.AccountStatus;

public record AdminMemberProfileRequest(
        String name,
        String email,
        AccountStatus status
) {}