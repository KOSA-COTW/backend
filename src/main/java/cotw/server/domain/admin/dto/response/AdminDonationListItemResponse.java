package cotw.server.domain.admin.dto.response;

import cotw.server.domain.payment.entity.PaymentStatus;

import java.time.LocalDateTime;

public record AdminDonationListItemResponse(
        long id,
        String member,
        String post,
        int amount,
        PaymentStatus status,
        String method,
        LocalDateTime createdAt
) {}