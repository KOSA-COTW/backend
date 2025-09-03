package cotw.server.domain.payment.exception;

import cotw.server.domain.board.exception.PostHasPaymentHistoryException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class PaymentExceptionHandler {

    @ExceptionHandler(PaymentIdempotencyException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentIdempotencyException(PaymentIdempotencyException e) {
        log.warn("Payment idempotency violation: {}", e.getMessage());

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.CONFLICT.value());
        errorResponse.put("error", "Payment Idempotency Error");
        errorResponse.put("errorCode", e.getErrorCode());
        errorResponse.put("message", e.getMessage());
        errorResponse.put("retryable", false);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentException(PaymentException e) {
        log.error("Payment exception occurred: {}", e.getMessage(), e);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "Payment Error");
        errorResponse.put("errorCode", e.getErrorCode());
        errorResponse.put("message", e.getMessage());

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(PostHasPaymentHistoryException.class)
    public ResponseEntity<Map<String, Object>> handlePostHasPaymentHistoryException(PostHasPaymentHistoryException e) {
        log.warn("Attempt to delete post with payment history: {}", e.getMessage());

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "Post Deletion Error");
        errorResponse.put("message", e.getMessage());

        return ResponseEntity.badRequest().body(errorResponse);
    }
}
