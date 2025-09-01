package cotw.server.domain.board.repository;

import cotw.server.domain.board.entity.Category;
import cotw.server.domain.board.entity.Post;
import cotw.server.domain.board.entity.PostVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 특정 게시글만 조회
    List<Post> findAllByVisibilityStatus(PostVisibility visibilityStatus);

    // 특정 작성자의 모든 게시글
    List<Post> findAllByAuthor_Email(String email);

    // ID 기반 조회
    Optional<Post> findById(Long id);

    // 메인용: 승인된 글만 + 마감 임박순 + 생성일 최신순
    @Query("""
           SELECT p
           FROM Post p
           WHERE p.visibilityStatus = 'APPROVED'
             AND p.deadline >= :today
           ORDER BY p.deadline ASC, p.createdAt DESC
           """)
    List<Post> findHomePosts(@Param("today") LocalDate today, Pageable pageable);

    // 카테고리 + 상태별 조회 (단순 메서드)
    Page<Post> findByCategoryAndVisibilityStatus(Category category, PostVisibility visibilityStatus, Pageable pageable);

    // 카테고리만 필터링 (상태 상관없이)
    Page<Post> findByCategory(Category category, Pageable pageable);
}