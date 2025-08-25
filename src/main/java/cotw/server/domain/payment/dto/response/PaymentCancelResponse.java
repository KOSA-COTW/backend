package cotw.server.domain.payment.dto.response;

import cotw.server.domain.payment.entity.PaymentStatus;
import cotw.server.domain.payment.entity.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCancelResponse {
    
    private String orderId;         // 주문 ID
    private String paymentKey;      // 결제 키
    private PaymentStatus status;   // 결제 상태 (CANCELED)
    private PaymentType type;       // 결제 타입
    private Integer totalAmount;    // 총 결제 금액
    private Integer cancelAmount;   // 취소 금액
    private Integer balanceAmount;  // 잔여 금액
    private String cancelReason;    // 취소 사유
    private OffsetDateTime canceledAt; // 취소 시각
    private String orderName;       // 주문명
}