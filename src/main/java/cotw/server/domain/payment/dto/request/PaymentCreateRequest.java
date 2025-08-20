package cotw.server.domain.payment.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PaymentCreateRequest {

    private Long postId;
    private Integer amount;
    private String orderId; // 프론트엔드에서 생성한 orderId
}
