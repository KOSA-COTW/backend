package cotw.server.domain.payment.repository;

import cotw.server.domain.payment.entity.PaymentLedger;
import cotw.server.domain.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentLedgerRepository extends JpaRepository<PaymentLedger, Long> {

    List<PaymentLedger> findByMemberIdOrderByCreatedAtDesc(Long memberId);
    List<PaymentLedger> findByPostIdOrderByCreatedAtDesc(Long postId);
    Optional<PaymentLedger> findByOrderId(String orderId);
    List<PaymentLedger> findByMemberIdAndStatus(Long memberId, PaymentStatus status);
}
