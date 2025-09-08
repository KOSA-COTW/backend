package cotw.server.domain.admin.dto.response;

import java.util.List;

public record AdminDonationListResponse(
        List<AdminDonationListItemResponse> content,
        long totalElements
) {}