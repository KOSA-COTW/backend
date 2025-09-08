package cotw.server.domain.donation.service;

import cotw.server.domain.donation.dto.PaymentCountedEvent;
import cotw.server.domain.donation.dto.PaymentReversedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class PaymentEventPublisher {
    private final ApplicationEventPublisher publisher;
    public void publishCounted(Long ledgerId, Long postId, long amount, LocalDate paidDate) {
        publisher.publishEvent(new PaymentCountedEvent(ledgerId, postId, amount, paidDate));
    }
    public void publishReversed(Long ledgerId, Long postId, long amount, LocalDate paidDate) {
        publisher.publishEvent(new PaymentReversedEvent(ledgerId, postId, amount, paidDate));
    }
}