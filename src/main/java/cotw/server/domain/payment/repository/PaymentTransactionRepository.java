package cotw.server.domain.payment.repository;

import cotw.server.domain.payment.entity.PaymentTransaction;
import cotw.server.domain.payment.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    // 특정 orderId의 모든 트랜잭션 로그
    List<PaymentTransaction> findByOrderIdOrderByCreatedAtDesc(String orderId);

    // 특정 사용자의 트랜잭션 로그
    List<PaymentTransaction> findByActionByOrderByCreatedAtDesc(String actionBy);

    // 특정 기간의 트랜잭션 로그 (감사용)
    @Query("""
           SELECT pt FROM PaymentTransaction pt
           WHERE pt.createdAt BETWEEN :startDate AND :endDate
           ORDER BY pt.createdAt DESC
           """)
    Page<PaymentTransaction> findByCreatedAtBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    // 실패한 트랜잭션 조회 (모니터링용)
    List<PaymentTransaction> findByResultAndCreatedAtAfterOrderByCreatedAtDesc(
            cotw.server.domain.payment.entity.TransactionResult result,
            LocalDateTime since);

    // 특정 타입의 트랜잭션 조회
    List<PaymentTransaction> findByTypeAndOrderIdOrderByCreatedAtDesc(
            TransactionType type, String orderId);
}