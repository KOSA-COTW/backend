package cotw.server.domain.payment.service;

import cotw.server.domain.payment.dto.response.PaymentHistoryResponse;
import cotw.server.domain.payment.entity.PaymentLedger;
import cotw.server.domain.payment.entity.PaymentOrder;
import cotw.server.domain.payment.repository.PaymentLedgerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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
                    .originalCreatedAt(LocalDateTime.now())
                    .paymentMethod(paymentOrder.getPaymentMethod())
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

    @Transactional(readOnly = true)
    public List<PaymentHistoryResponse> getPaymentHistoryByMember(Long memberId) {
        List<PaymentLedger> ledgers = paymentLedgerRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
        return ledgers.stream()
                .map(PaymentHistoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentHistoryResponse> getPaymentHistoryByPost(Long postId) {
        List<PaymentLedger> ledgers = paymentLedgerRepository.findByPostIdOrderByCreatedAtDesc(postId);
        return ledgers.stream()
                .map(PaymentHistoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Async
    @Transactional
    public void updateLedgerToCanceledAsync(PaymentOrder paymentOrder, String cancelReason) {
        try {
            PaymentLedger existingLedger = paymentLedgerRepository.findByOrderId(paymentOrder.getOrderId())
                    .orElseThrow(() -> new RuntimeException("PaymentLedger not found for orderId: " + paymentOrder.getOrderId()));
            
            existingLedger.updateToCanceled(cancelReason, LocalDateTime.now());
            paymentLedgerRepository.save(existingLedger);
            
            log.info("PaymentLedger updated to CANCELED for orderId: {}", paymentOrder.getOrderId());
        } catch (Exception e) {
            log.error("Failed to update PaymentLedger to CANCELED for orderId: {}", paymentOrder.getOrderId(), e);
        }
    }
}
