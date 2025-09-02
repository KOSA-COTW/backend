package cotw.server.domain.payment.repository;


import cotw.server.domain.member.entity.Member;
import cotw.server.domain.payment.entity.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import cotw.server.domain.admin.dto.response.AdminTopDonorResponse;
import cotw.server.domain.payment.entity.PaymentOrder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import cotw.server.domain.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

           
    // ===== 관리자 통계 =====

    /** 총 기부액 (성공 건만: PaymentStatus.DONE) */
    @Query("""
        select coalesce(sum(po.amount), 0)
          from PaymentOrder po
         where po.status = cotw.server.domain.payment.entity.PaymentStatus.DONE
    """)
    long sumAllDone();


    @Query("""
        select new cotw.server.domain.admin.dto.response.AdminTopDonorResponse(
            po.member.id,
            po.member.name,
            coalesce(sum(po.amount), 0)
        )
          from PaymentOrder po
         where po.status = cotw.server.domain.payment.entity.PaymentStatus.DONE
         group by po.member.id, po.member.name
         order by sum(po.amount) desc
    """)
    List<AdminTopDonorResponse> findTopDonors(Pageable pageable);

    /** 편의 메서드: 상위 N명 */
    default List<AdminTopDonorResponse> findTopDonors(int limit) {
        return findTopDonors(PageRequest.of(0, limit));
    }

}
