package cotw.server.domain.admin.dto.response;

import java.time.LocalDateTime;

public record AdminRecentPaymentResponse(
        String donorEmail,
        long amount,
        String status,
        LocalDateTime at
) {}