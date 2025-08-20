package cotw.server.domain.payment.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentCreateResponse {

    private String orderId;
    private Integer amount;
    private String orderName;
}
