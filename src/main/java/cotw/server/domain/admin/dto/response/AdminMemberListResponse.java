package cotw.server.domain.admin.dto.response;

import java.util.List;

public record AdminMemberListResponse(
        List<AdminMemberListItemResponse> content,
        long totalElements
) {}