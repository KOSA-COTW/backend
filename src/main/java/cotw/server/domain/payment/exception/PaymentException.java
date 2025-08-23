package cotw.server.domain.payment.exception;

import lombok.Getter;

@Getter
public class PaymentException extends RuntimeException {
    private final String errorCode;

    public PaymentException(String message) {
        super(message);
        this.errorCode = "PAYMENT_ERROR";
    }

    public PaymentException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
