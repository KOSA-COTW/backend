package cotw.server.domain.payment.repository;

import cotw.server.domain.payment.entity.PaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEvent, Long> {

    Optional<PaymentEvent> findByOrderId(String orderId);
}
