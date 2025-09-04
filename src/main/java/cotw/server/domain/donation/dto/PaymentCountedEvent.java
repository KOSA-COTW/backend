package cotw.server.domain.donation.dto;

import java.time.LocalDate;

public record PaymentCountedEvent(
        Long ledgerId,
        Long postId,
        long amountWon,
        LocalDate paidDate
) {

}

