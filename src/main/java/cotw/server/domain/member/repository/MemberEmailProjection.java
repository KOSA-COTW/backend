package cotw.server.domain.member.repository;

import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MemberEmailProjection {
    Long getId();
    String getEmail();
}


