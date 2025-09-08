package cotw.server.domain.admin.dto.response;

import java.util.List;

public record AdminDailyDonationResponse(
        List<String> labels,
        List<Long> values
) {}