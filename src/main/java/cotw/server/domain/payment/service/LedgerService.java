package cotw.server.domain.payment.service;

import cotw.server.domain.payment.entity.PaymentLedger;
import cotw.server.domain.payment.entity.PaymentOrder;
import cotw.server.domain.payment.entity.PaymentStatus;
import cotw.server.domain.payment.repository.PaymentLedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerService {

    private final PaymentLedgerRepository paymentLedgerRepository;

    @Async
    @Transactional
    public void createPaymentLedgerAsync(PaymentOrder paymentOrder) {
        try {
            PaymentLedger ledger = PaymentLedger.builder()
                    .orderId(paymentOrder.getOrderId())
                    .paymentKey(paymentOrder.getPaymentKey())
                    .memberId(paymentOrder.getMember().getId())
                    .postId(paymentOrder.getPost().getId())
                    .amount(paymentOrder.getAmount())
                    .status(paymentOrder.getStatus())
                    .type(paymentOrder.getType())
                    .memberName(paymentOrder.getMember().getName())
                    .postTitle(paymentOrder.getPost().getTitle())
                    .build();

            paymentLedgerRepository.save(ledger);
            log.info("PaymentLedger created successfully for orderId: {}", paymentOrder.getOrderId());
        } catch (Exception e) {
            log.error("Failed to create PaymentLedger for orderId: {}", paymentOrder.getOrderId(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<PaymentLedger> getPaymentLedgersByMember(Long memberId) {
        return paymentLedgerRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
    }

    @Transactional(readOnly = true)
    public List<PaymentLedger> getPaymentLedgersByPost(Long postId) {
        return paymentLedgerRepository.findByPostIdOrderByCreatedAtDesc(postId);
    }

    @Async
    @Transactional
    public void createCancellationLedgerAsync(PaymentOrder paymentOrder, String cancelReason) {
        try {
            PaymentLedger cancellationLedger = PaymentLedger.builder()
                    .orderId(paymentOrder.getOrderId() + "_CANCEL")
                    .paymentKey(paymentOrder.getPaymentKey())
                    .memberId(paymentOrder.getMember().getId())
                    .postId(paymentOrder.getPost().getId())
                    .amount(-paymentOrder.getAmount()) // 음수로 기록하여 취소를 표시
                    .status(PaymentStatus.CANCELED) // 취소 상태로 명시
                    .type(paymentOrder.getType())
                    .memberName(paymentOrder.getMember().getName())
                    .postTitle(paymentOrder.getPost().getTitle() + " (취소: " + cancelReason + ")")
                    .build();

            paymentLedgerRepository.save(cancellationLedger);
            log.info("Cancellation PaymentLedger created successfully for orderId: {}", paymentOrder.getOrderId());
        } catch (Exception e) {
            log.error("Failed to create cancellation PaymentLedger for orderId: {}", paymentOrder.getOrderId(), e);
        }
    }
}
