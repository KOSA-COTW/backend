package cotw.server.domain.member.repository;

import cotw.server.domain.member.entity.Member;
import cotw.server.domain.member.entity.ProviderType;
import cotw.server.domain.member.entity.AccountStatus;
import cotw.server.domain.member.entity.Role;
import org.springframework.data.domain.Page;
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

    Optional<Member> findEmailById(Long id);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("select m.id as id, m.email as email from Member m where m.id in :ids")
    List<MemberEmailProjection> findEmailsByIdIn(@Param("ids") List<Long> ids);

    Optional<Member> findByVerifiedEmail(String verifiedEmail);

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String login);

    boolean existsByVerifiedEmailIgnoreCase(String verify);

    Optional<Member> findByEmailOrVerifiedEmail(String email, String verifiedEmail);

    boolean existsByNicknameIgnoreCase(String nickname);

    boolean existsByEmailIgnoreCaseAndStatus(String email, AccountStatus status);

    @Query(value = """
        select m from Member m
        where m.status <> cotw.server.domain.member.entity.AccountStatus.DELETED
          and ((:keyword is null or :keyword = '') 
               or lower(m.name) like :keyword 
               or lower(m.email) like :keyword)
          and (:role is null or m.role = :role)
          and (:status is null or m.status = :status)
        order by m.createdAt desc, m.id desc
    """,
            countQuery = """
        select count(m) from Member m
        where m.status <> cotw.server.domain.member.entity.AccountStatus.DELETED
          and ((:keyword is null or :keyword = '') 
               or lower(m.name) like :keyword 
               or lower(m.email) like :keyword)
          and (:role is null or m.role = :role)
          and (:status is null or m.status = :status)
    """)
    Page<Member> searchMembers(@Param("keyword") String keyword,
                               @Param("role") Role role,
                               @Param("status") AccountStatus status,
                               Pageable pageable);

    //  전체 회원 수 (탈퇴 회원 제외)
    @Query("select count(m) from Member m where m.status <> cotw.server.domain.member.entity.AccountStatus.DELETED")
    long countActiveMembers();
}
