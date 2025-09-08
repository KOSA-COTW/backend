package cotw.server.common.log.repository;

import cotw.server.common.log.entity.AccountEventLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountEventLogRepository extends JpaRepository<AccountEventLog, Long> { }