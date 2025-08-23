package cotw.server.domain.payment.exception;

public class PaymentIdempotencyException extends PaymentException {
    
    public PaymentIdempotencyException(String message) {
        super(message, "PAYMENT_IDEMPOTENCY_ERROR");
    }
}