package cotw.server.domain.board.repository;

import cotw.server.domain.board.entity.Category;
import cotw.server.domain.board.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // 관리자용: 비공개 게시글 조회 (카테고리 필터, 정렬, 페이징)
    @Query("""
           SELECT p
           FROM Post p
           WHERE p.isPublic = false
             AND (:category IS NULL OR p.category = :category)
           ORDER BY 
             CASE WHEN :sortDirection = 'ASC' THEN p.createdAt END ASC,
             CASE WHEN :sortDirection = 'DESC' THEN p.createdAt END DESC
           """)
    Page<Post> findAdminOnlyPosts(
        @Param("category") Category category, 
        @Param("sortDirection") String sortDirection,
        Pageable pageable
    );

    // 관리자용: 공개 게시글 조회 (카테고리 필터, 정렬, 페이징)
    @Query("""
           SELECT p
           FROM Post p
           WHERE p.isPublic = true
             AND (:category IS NULL OR p.category = :category)
           ORDER BY 
             CASE WHEN :sortDirection = 'ASC' THEN p.createdAt END ASC,
             CASE WHEN :sortDirection = 'DESC' THEN p.createdAt END DESC
           """)
    Page<Post> findAdminOnlyPublicPosts(
        @Param("category") Category category, 
        @Param("sortDirection") String sortDirection,
        Pageable pageable
    );
}
