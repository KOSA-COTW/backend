package cotw.server.domain.payment.repository;

import cotw.server.domain.payment.entity.PaymentEvent;
import cotw.server.domain.payment.entity.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEvent, Long> {

    Optional<PaymentEvent> findByOrderId(String orderId);

    @Query("SELECT po FROM PaymentOrder po WHERE po.member.id = :memberId")
    List<PaymentOrder> findOrdersByMemberId(@Param("memberId") Long memberId);

    @Query("SELECT po FROM PaymentOrder po WHERE po.post.id = :postId")
    List<PaymentOrder> findOrdersByPostId(@Param("postId") Long postId);
}
