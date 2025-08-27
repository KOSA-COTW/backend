package cotw.server.domain.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cotw.server.domain.payment.entity.PaymentTransaction;
import cotw.server.domain.payment.entity.TransactionResult;
import cotw.server.domain.payment.entity.TransactionType;
import cotw.server.domain.payment.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final PaymentTransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;

    @Async
    @Transactional
    public void logTransactionAsync(String orderId, TransactionType type, String actionBy,
                                   String paymentKey, Integer amount, String apiEndpoint) {
        try {
            PaymentTransaction transaction = PaymentTransaction.builder()
                    .orderId(orderId)
                    .type(type)
                    .actionBy(actionBy)
                    .paymentKey(paymentKey)
                    .amount(amount)
                    .apiEndpoint(apiEndpoint)
                    .result(TransactionResult.SUCCESS)
                    .build();

            transactionRepository.save(transaction);
            log.info("Transaction logged: orderId={}, type={}, actionBy={}", orderId, type, actionBy);
        } catch (Exception e) {
            log.error("Failed to log transaction: orderId={}, type={}", orderId, type, e);
        }
    }

    @Transactional
    public PaymentTransaction logTransactionSync(String orderId, TransactionType type, String actionBy,
                                               String paymentKey, Integer amount, String apiEndpoint) {
        PaymentTransaction transaction = PaymentTransaction.builder()
                .orderId(orderId)
                .type(type)
                .actionBy(actionBy)
                .paymentKey(paymentKey)
                .amount(amount)
                .apiEndpoint(apiEndpoint)
                .result(TransactionResult.SUCCESS)
                .build();

        return transactionRepository.save(transaction);
    }

    @Transactional
    public void updateTransactionResult(Long transactionId, TransactionResult result, 
                                       Object responsePayload, String errorMessage) {
        transactionRepository.findById(transactionId).ifPresent(transaction -> {
            if (result == TransactionResult.SUCCESS) {
                transaction.markAsSuccess(convertToJson(responsePayload));
            } else {
                transaction.markAsFailure(errorMessage);
            }
            transactionRepository.save(transaction);
        });
    }

    @Transactional(readOnly = true)
    public List<PaymentTransaction> getTransactionsByOrderId(String orderId) {
        return transactionRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
    }

    @Transactional(readOnly = true) 
    public List<PaymentTransaction> getTransactionsByActionBy(String actionBy) {
        return transactionRepository.findByActionByOrderByCreatedAtDesc(actionBy);
    }

    private String convertToJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Failed to convert object to JSON", e);
            return obj.toString();
        }
    }
}