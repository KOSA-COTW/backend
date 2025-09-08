package cotw.server.domain.admin.dto.response;

import cotw.server.domain.member.entity.AccountStatus;
import java.time.LocalDateTime;
import java.util.List;

public record AdminMemberListItemResponse(
        long id,
        String name,
        String email,
        List<String> roles,
        AccountStatus status,
        LocalDateTime createdAt
) {}