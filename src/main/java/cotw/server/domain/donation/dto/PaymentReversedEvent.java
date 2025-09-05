package cotw.server.domain.donation.dto;

import java.time.LocalDate;

public record PaymentReversedEvent(
        Long ledgerId,
        Long postId,
        long amountWon,
        LocalDate paidDate
) {

}

