package cotw.server.domain.payment.entity;

import cotw.server.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payment_transactions")
public class PaymentTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false)
    private String actionBy;  // 누가 실행했는지 (memberId 또는 "SYSTEM")

    @Column
    private String paymentKey;  // 결제 키

    @Column
    private Integer amount;  // 금액

    @Column(columnDefinition = "TEXT")
    private String responsePayload;  // 응답 데이터 JSON

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TransactionResult result = TransactionResult.SUCCESS;  // 기본값 SUCCESS

    @Column
    private String errorMessage;  // 실패 시 에러 메시지

    @Column
    private String apiEndpoint;   // 호출한 API 엔드포인트

    public void markAsSuccess(String responsePayload) {
        this.result = TransactionResult.SUCCESS;
        this.responsePayload = responsePayload;
    }

    public void markAsFailure(String errorMessage) {
        this.result = TransactionResult.FAILURE;
        this.errorMessage = errorMessage;
    }
}