package cotw.server.domain.payment.repository;

import cotw.server.domain.payment.entity.PaymentLedger;
import cotw.server.domain.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentLedgerRepository extends JpaRepository<PaymentLedger, Long> {

    List<PaymentLedger> findByMemberIdOrderByCreatedAtDesc(Long memberId);
    List<PaymentLedger> findByPostIdOrderByCreatedAtDesc(Long postId);
    Optional<PaymentLedger> findByOrderId(String orderId);


    List<PaymentLedger> findByMemberIdAndStatus(Long memberId, PaymentStatus status);

    Optional<PaymentLedger> findByPaymentKey(String paymentKey);

    @Query("select coalesce(sum(p.amount),0) from PaymentLedger p where p.status = :status")
    Long sumByStatus(@Param("status") PaymentStatus status); // 재계산용

    @Query("select coalesce(sum(p.amount),0) from PaymentLedger p where p.status = 'DONE'")
    Long sumDoneAll();

    @Query("select coalesce(sum(p.amount),0) from PaymentLedger p where p.status = 'DONE' and p.postId = :postId")
    Long sumDoneByPost(@Param("postId") Long postId);

    @Query("""
           select coalesce(sum(p.amount),0)
           from PaymentLedger p
           where p.status = 'DONE'
             and p.originalCreatedAt >= :start and p.originalCreatedAt < :end
           """)
    Long sumDoneByDateRange(@Param("start") LocalDateTime start,
                            @Param("end") LocalDateTime end);

    @Query("""
           select p.memberId, p.memberName, coalesce(sum(p.amount), 0) as totalAmount
           from PaymentLedger p
           where p.status = 'DONE'
           group by p.memberId, p.memberName
           order by totalAmount desc
           limit :limit
           """)
    List<Object[]> findTopDonorsByAmount(@Param("limit") int limit);

    @Query("""
           select post.category, coalesce(sum(p.amount), 0) as totalAmount
           from PaymentLedger p
           join Post post on p.postId = post.id
           where p.status = 'DONE'
           group by post.category
           order by totalAmount desc
           """)
    List<Object[]> findDonationAmountByCategory();


}
