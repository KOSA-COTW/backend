package cotw.server.domain.member.repository;

import cotw.server.domain.member.entity.Member;
import cotw.server.domain.member.entity.ProviderType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    Optional<Member> findByProviderAndProviderId(ProviderType provider, String providerId);

    Optional<Object> findByIdAndPassword(Long id, String password);

    @Query("""
       select m.id from Member m
       where m.status = 'DELETED'
         and m.retentionUntil is not null
         and m.retentionUntil < :now
    """)
    List<Long> findIdsToHardDelete(@Param("now") LocalDateTime now, Pageable pageable);

    @Modifying
    @Query("delete from Member m where m.id in :ids")
    void deleteByIdIn(@Param("ids") List<Long> ids);

}
