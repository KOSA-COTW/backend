package cotw.server.domain.board.repository;

import cotw.server.domain.board.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findAllByIsPublicTrue();
    List<Post> findAllByAuthor_Email(String email);
    Optional<Post> findById(Long id);
}
