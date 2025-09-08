package cotw.server.domain.admin.dto.response;

import java.time.LocalDateTime;

public record AdminTopDonorDashboardResponse(
        int rank,
        String memberName,
        String memberEmail,
        long totalAmount,
        long donationCount,
        LocalDateTime lastDonationDate
) {}