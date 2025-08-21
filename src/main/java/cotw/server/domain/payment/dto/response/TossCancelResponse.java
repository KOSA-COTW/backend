package cotw.server.domain.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TossCancelResponse {
    
    private String orderId;
    private String paymentKey;
    private String status;
    private String method;
    private Integer totalAmount;
    private Integer balanceAmount;
    private Integer suppliedAmount;
    private Integer vat;
    private Integer taxFreeAmount;
    private String currency;
    private String orderName;
    private OffsetDateTime requestedAt;
    private OffsetDateTime approvedAt;
    private String receiptUrl;
    
    // 취소 관련 정보
    private List<Cancel> cancels;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Cancel {
        private Integer cancelAmount;
        private String cancelReason;
        private Integer taxFreeAmount;
        private Integer taxExemptionAmount;
        private Integer refundableAmount;
        private Integer easyPayDiscountAmount;
        private OffsetDateTime canceledAt;
        private String transactionKey;
        private String receiptKey;
    }
}