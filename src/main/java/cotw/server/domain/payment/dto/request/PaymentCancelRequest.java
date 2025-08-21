package cotw.server.domain.payment.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCancelRequest {
    
    private String paymentKey;      // 결제 키 (프론트엔드에서 전달)
    
    // 취소 사유와 취소 금액은 백엔드에서 자동 설정
    public String getCancelReason() {
        return "단순 변심";
    }
    
    public Integer getCancelAmount() {
        return null; // null이면 전액 취소
    }
}
