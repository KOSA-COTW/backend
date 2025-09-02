package cotw.server.domain.board.repository;

import cotw.server.domain.board.entity.Category;
import cotw.server.domain.board.entity.Post;
import cotw.server.domain.member.entity.Member;
import cotw.server.domain.board.entity.PostVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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


    // 관리자용: 공개 게시글 조회 (카테고리 필터, 정렬, 페이징)
    @Query("""
           SELECT p
           FROM Post p
           WHERE p.visibilityStatus = 'APPROVED'
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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Post p
           set p.author = :deletedUser
         where p.author.id in :memberIds
    """)
    int anonymizeAuthorByMemberIds(@Param("memberIds") List<Long> memberIds,
                                   @Param("deletedUser") Member deletedUser);

    // 카테고리만 필터링 (상태 상관없이)
    Page<Post> findByCategory(Category category, Pageable pageable);
}

