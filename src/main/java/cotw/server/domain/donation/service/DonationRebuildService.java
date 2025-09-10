package cotw.server.domain.donation.service;

import cotw.server.domain.payment.entity.PaymentStatus;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DonationRebuildService {
    /*
     EntityManager는 이름 그대로, @Entity 어노테이션을 달고 있는 Entity 객체들을 관리하며 실제 DB 테이블과
     매핑하여 데이터를 조회/수정/저장 하는 중요한 기능을 수행한다.
     EntityManager는 PersistenceContext라는 논리적 영역을 두어, 내부적으로 Entity의 생애주기를 관리한다.
     */
    private final EntityManager em;
    private final StringRedisTemplate redis;

    @Transactional(readOnly = true)
    public void rebuildAll() {
        // 전체 합 (DONE만)
        Long total = em.createQuery("""
            select coalesce(sum(p.amount),0)
            from PaymentLedger p
            where p.status = :s
        """, Long.class).setParameter("s", PaymentStatus.DONE).getSingleResult();
        redis.opsForValue().set("donation:total", String.valueOf(total));

        // 게시글별 합
        List<Object[]> rows = em.createQuery("""
            select p.postId, coalesce(sum(p.amount),0)
            from PaymentLedger p
            where p.status = :s
            group by p.postId
        """, Object[].class).setParameter("s", PaymentStatus.DONE).getResultList();
        for (Object[] r : rows) {
            Long postId = (Long) r[0];
            Long sum = (Long) r[1];
            redis.opsForValue().set("donation:post:" + postId + ":total", String.valueOf(sum));
        }
    }
}

