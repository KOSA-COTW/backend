package cotw.server.domain.payment.entity;

import cotw.server.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payment_ledgers")
public class PaymentLedger extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String orderId;

    @Column(nullable = false)
    private String paymentKey;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private Long postId;

    @Column(nullable = false)
    private Integer amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType type;

    @Column(nullable = false)
    private String memberName;

    @Column(nullable = false)
    private String postTitle;
    
    @Column
    private LocalDateTime originalCreatedAt;  // 원래 결제일
    
    @Column
    private LocalDateTime canceledAt;         // 취소일 (취소된 경우)
    
    @Column
    private String cancelReason;              // 취소 사유
    
    @Column
    private String paymentMethod;             // 결제 방법 (토스에서 받은 method 값)
    
    public void updateToCanceled(String cancelReason, LocalDateTime canceledAt) {
        this.status = PaymentStatus.CANCELED;
        this.cancelReason = cancelReason;
        this.canceledAt = canceledAt;
    }
}
