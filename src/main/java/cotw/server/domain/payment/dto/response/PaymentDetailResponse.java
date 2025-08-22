package cotw.server.domain.payment.dto.response;

import cotw.server.domain.payment.entity.PaymentStatus;
import cotw.server.domain.payment.entity.PaymentType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PaymentDetailResponse {

    private Long id;
    private String orderId;
    private String paymentKey;
    private String memberName;
    private String postTitle;
    private Integer amount;
    private PaymentStatus status;
    private PaymentType type;
    private LocalDateTime createdAt;
    private LocalDateTime originalCreatedAt;  // 원래 결제일
    private LocalDateTime canceledAt;         // 취소일 (취소된 경우)
    private String cancelReason;              // 취소 사유
}
