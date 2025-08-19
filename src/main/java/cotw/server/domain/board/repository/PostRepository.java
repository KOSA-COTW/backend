package cotw.server.domain.board.repository;

import cotw.server.domain.board.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findAllByIsPublicTrue();
    List<Post> findAllByAuthor_Email(String email);
    Optional<Post> findById(Long id);
    // 메인용: 공개 +  마감 임박 순 + 생성일 최신순(동률 깨기)
    @Query("""
           SELECT p
           FROM Post p
           WHERE p.isPublic = true
             AND p.deadline >= :today
           ORDER BY p.deadline ASC, p.createdAt DESC
           """)
    List<Post> findHomePosts(LocalDate today, Pageable pageable);
}
