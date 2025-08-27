package cotw.server.domain.payment.repository;

import cotw.server.domain.payment.dto.response.PaymentDetailResponse;
import cotw.server.domain.payment.entity.PaymentOrder;
import cotw.server.domain.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    Optional<PaymentOrder> findByOrderId(String orderId);
    Optional<PaymentOrder> findByPaymentKey(String paymentKey);
    List<PaymentOrder> findByMemberIdOrderByCreatedAtDesc(Long memberId);
    List<PaymentOrder> findByPostIdOrderByCreatedAtDesc(Long postId);
    boolean existsByOrderId(String orderId);

    List<PaymentDetailResponse> findByMemberIdAndStatus(Long memberId, PaymentStatus status);
}
