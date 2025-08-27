package cotw.server.domain.payment.dto.response;

import cotw.server.domain.payment.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentHistoryResponse {

    private Long id;
    private String postTitle;
    private LocalDateTime createdAt;
    private Integer amount;
    private PaymentStatus status;
    private String orderId;
    private String paymentMethod;
    private Long postId;
    private String memberName;
    private String paymentKey;

    // 취소 관련 정보
    private LocalDateTime canceledAt;
    private String cancelReason;

    public static PaymentHistoryResponse fromEntity(cotw.server.domain.payment.entity.PaymentLedger ledger) {
        return PaymentHistoryResponse.builder()
                .id(ledger.getId())
                .postTitle(ledger.getPostTitle())
                .createdAt(ledger.getCreatedAt())
                .amount(ledger.getAmount())
                .status(ledger.getStatus())
                .orderId(ledger.getOrderId())
                .paymentMethod(ledger.getPaymentMethod())
                .postId(ledger.getPostId())
                .memberName(ledger.getMemberName())
                .paymentKey(ledger.getPaymentKey())
                .canceledAt(ledger.getCanceledAt())
                .cancelReason(ledger.getCancelReason())
                .build();
    }
}