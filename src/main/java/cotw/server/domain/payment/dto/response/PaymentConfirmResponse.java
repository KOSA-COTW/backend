package cotw.server.domain.payment.dto.response;

import cotw.server.domain.payment.entity.PaymentStatus;
import cotw.server.domain.payment.entity.PaymentType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentConfirmResponse {

    private String orderId;
    private String paymentKey;
    private PaymentStatus status;
    private PaymentType type;
    private Integer amount;
    private String orderName;
}
