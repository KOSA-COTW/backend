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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Post p
           set p.author = :deletedUser
         where p.author.id in :memberIds
    """)
    int anonymizeAuthorByMemberIds(@Param("memberIds") List<Long> memberIds,
                                   @Param("deletedUser") Member deletedUser);

    @Query("""
   SELECT DISTINCT p
   FROM Post p
   LEFT JOIN FETCH p.images
   WHERE p.id = :id
   """)
    Optional<Post> findDetailById(@Param("id") Long id);

    @Query(value = """
    SELECT p.* 
    FROM post p
    LEFT JOIN member a ON a.member_id = p.member_id
    WHERE p.visibility_status = 'APPROVED'
      AND (COALESCE(:category, '') = '' OR p.category = :category)
      AND (COALESCE(:title, '') = '' OR LOWER(p.title) LIKE LOWER(CONCAT('%', :title, '%')))
      AND (COALESCE(:authorName, '') = '' OR LOWER(a.name) LIKE LOWER(CONCAT('%', :authorName, '%')))
      AND (
        :fundStatus = '' 
        OR (:fundStatus = 'ONGOING'  AND p.deadline >= CURRENT_DATE)
        OR (:fundStatus = 'COMPLETED' AND p.deadline < CURRENT_DATE)
      )
    ORDER BY 
      CASE WHEN :sortBy = 'date' AND :sortDirection = 'desc' THEN p.created_at END DESC,
      CASE WHEN :sortBy = 'date' AND :sortDirection = 'asc'  THEN p.created_at END ASC,
      CASE WHEN :sortBy = 'title' AND :sortDirection = 'desc' THEN p.title END DESC,
      CASE WHEN :sortBy = 'title' AND :sortDirection = 'asc'  THEN p.title END ASC
    """,
            countQuery = """
    SELECT COUNT(*) 
    FROM post p
    LEFT JOIN member a ON a.member_id = p.member_id
    WHERE p.visibility_status = 'APPROVED'
      AND (COALESCE(:category, '') = '' OR p.category = :category)
      AND (COALESCE(:title, '') = '' OR LOWER(p.title) LIKE LOWER(CONCAT('%', :title, '%')))
      AND (COALESCE(:authorName, '') = '' OR LOWER(a.name) LIKE LOWER(CONCAT('%', :authorName, '%')))
      AND (
        :fundStatus = '' 
        OR (:fundStatus = 'ONGOING'  AND p.deadline >= CURRENT_DATE)
        OR (:fundStatus = 'COMPLETED' AND p.deadline < CURRENT_DATE)
      )
    """,
            nativeQuery = true)
    Page<Post> findAllApprovedWithFilters(
            @Param("category") String category,
            @Param("title") String title,
            @Param("authorName") String authorName,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection,
            @Param("fundStatus") String fundStatus,
            Pageable pageable
    );



}

