package cotw.server.domain.payment.repository;

import cotw.server.domain.member.entity.Member;
import cotw.server.domain.payment.entity.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update PaymentOrder p
           set p.member = :deletedUser
         where p.member.id in :memberIds
    """)
    int reassignMemberToDeleted(@Param("memberIds") List<Long> memberIds,
                                @Param("deletedUser") Member deletedUser);

}
