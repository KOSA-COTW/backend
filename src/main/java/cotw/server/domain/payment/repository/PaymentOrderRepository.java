package cotw.server.domain.payment.repository;

import cotw.server.domain.admin.dto.response.AdminDonationListItemResponse;
import cotw.server.domain.admin.dto.response.AdminTopDonorProjection;
import cotw.server.domain.admin.dto.response.AdminTopDonorResponse;
import cotw.server.domain.member.entity.Member;
import cotw.server.domain.payment.entity.PaymentOrder;
import cotw.server.domain.payment.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    boolean existsByPostId(Long postId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update PaymentOrder p
           set p.member = :deletedUser
         where p.member.id in :memberIds
    """)
    int reassignMemberToDeleted(@Param("memberIds") List<Long> memberIds,
                                @Param("deletedUser") Member deletedUser);

    // ===== 기존 관리자 통계 =====
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

    default List<AdminTopDonorResponse> findTopDonors(int limit) {
        return findTopDonors(PageRequest.of(0, limit));
    }

    // ===== 신규 대시보드 =====

    long countByStatus(PaymentStatus status);

    @Query("""
        select count(distinct po.member.id)
          from PaymentOrder po
         where po.status = :status
    """)
    long countDistinctMemberByStatus(@Param("status") PaymentStatus status);

    @Query("""
        select new cotw.server.domain.admin.dto.response.AdminTopDonorProjection(
            po.member.name,
            po.member.email,
            sum(po.amount),
            count(po),
            max(po.createdAt)
        )
          from PaymentOrder po
         where po.status = cotw.server.domain.payment.entity.PaymentStatus.DONE
         group by po.member.name, po.member.email
         order by sum(po.amount) desc
    """)
    List<AdminTopDonorProjection> findTopDonorProjections(Pageable pageable);

    // ✅ concat 제거, lower(...) like :search 로 변경
    @Query(value = """
        select new cotw.server.domain.admin.dto.response.AdminDonationListItemResponse(
            po.id,
            m.email,
            m.name,
            p.title,
            po.amount,
            po.status,
            po.paymentMethod,
            po.createdAt
        )
          from PaymentOrder po
          join po.member m
          join po.post p
         where (:search is null or lower(m.name) like :search or lower(p.title) like :search)
           and (:status is null or po.status = :status)
         order by po.createdAt desc
    """,
            countQuery = """
        select count(po)
          from PaymentOrder po
          join po.member m
          join po.post p
         where (:search is null or lower(m.name) like :search or lower(p.title) like :search)
           and (:status is null or po.status = :status)
    """)
    Page<AdminDonationListItemResponse> searchDonations(@Param("search") String search,
                                                        @Param("status") PaymentStatus status,
                                                        Pageable pageable);

}
