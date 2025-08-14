package cotw.server.domain.member.repository;

import aj.org.objectweb.asm.commons.Remapper;
import cotw.server.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    Remapper findByProviderAndProviderId(String attr0, String attr1);
}
