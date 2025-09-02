package cotw.server.domain.donation.DTO;

import java.time.LocalDate;

public record PaymentCountedEvent(
        Long ledgerId,
        Long postId,
        long amountWon,
        LocalDate paidDate
) {

}

