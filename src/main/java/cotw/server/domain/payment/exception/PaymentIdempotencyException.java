package cotw.server.domain.payment.exception;

public class PaymentIdempotencyException extends PaymentException {
    
    public PaymentIdempotencyException(String message) {
        super(message);
    }
    
    public PaymentIdempotencyException(String message, Throwable cause) {
        super(message, cause);
    }
}