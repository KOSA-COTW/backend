package cotw.server.domain.board.repository;

import cotw.server.domain.board.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice,Long> {
    long countByIsPinnedTrue();
}
