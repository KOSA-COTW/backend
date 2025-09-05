package cotw.server.domain.donation;

import cotw.server.domain.donation.dto.PaymentCountedEvent;
import cotw.server.domain.donation.dto.PaymentReversedEvent;
import cotw.server.domain.donation.service.DonationCounterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class DonationEventListener {
    private final DonationCounterService counters;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCounted(PaymentCountedEvent e) {
        counters.applyPaid(e.postId(), e.amountWon(), e.paidDate());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReversed(PaymentReversedEvent e) {
        counters.applyReversal(e.postId(), e.amountWon(), e.paidDate());
    }
}