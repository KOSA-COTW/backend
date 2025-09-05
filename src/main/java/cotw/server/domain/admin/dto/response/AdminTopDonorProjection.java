package cotw.server.domain.admin.dto.response;

import java.time.LocalDateTime;

public record AdminTopDonorProjection(
        String memberName,
        String memberEmail,
        long totalAmount,
        long donationCount,
        LocalDateTime lastDonationDate
) {}