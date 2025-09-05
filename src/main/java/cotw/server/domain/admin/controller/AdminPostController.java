package cotw.server.domain.admin.controller;

import cotw.server.common.jwt.CustomUserDetails;
import cotw.server.domain.admin.dto.request.AdminPostPageRequestDTO;
import cotw.server.domain.admin.dto.response.AdminPostCountResponseDTO;
import cotw.server.domain.admin.dto.response.AdminPostListResponseDto;
import cotw.server.domain.admin.dto.response.AdminPostPageResponseDTO;
import cotw.server.domain.admin.service.AdminPostService;
import cotw.server.domain.board.entity.Category;
import cotw.server.domain.board.entity.PostVisibility;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/posts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPostController {

    private final AdminPostService adminPostService;

    /**
     * 승인 대기 중인 게시글 조회
     */
    @GetMapping("/pending")
    public ResponseEntity<List<AdminPostListResponseDto>> getPendingPosts() {
        return ResponseEntity.ok(adminPostService.getPostsByStatus(PostVisibility.PENDING));
    }

    /**
     * 게시글 상태 변경 (승인/거절/비공개 등)
     */
    @PatchMapping("/status")
    public ResponseEntity<Void> changePostStatus(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Integer> postIds = (List<Integer>) body.get("postIds");
        String status = (String) body.get("status"); // "APPROVED", "REJECTED", "PRIVATE", "PENDING"
        String rejectionReason = (String) body.get("rejectionReason");

        adminPostService.changePostsVisibility(
                postIds.stream().map(Long::valueOf).toList(),
                PostVisibility.valueOf(status),
                principal.getUsername(),
                rejectionReason
        );
        return ResponseEntity.ok().build();
    }

    /**
     * 전체 게시글 조회 (페이징, 필터링, 정렬)
     */
    @GetMapping
    public ResponseEntity<AdminPostPageResponseDTO> getAllPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) PostVisibility visibility,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String authorName,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        AdminPostPageRequestDTO request = new AdminPostPageRequestDTO();
        request.setPage(page);
        request.setLimit(limit);
        request.setVisibility(visibility);
        request.setCategory(category);
        request.setTitle(title);
        request.setAuthorName(authorName);
        request.setSortBy(sortBy);
        request.setSortDirection(sortDirection);
        
        return ResponseEntity.ok(adminPostService.getAllPosts(request));
    }

    /**
     * 게시글 개수 통계 조회
     */
    @GetMapping("/count")
    public ResponseEntity<AdminPostCountResponseDTO> getPostCounts() {
        return ResponseEntity.ok(adminPostService.getPostCounts());
    }

    /**
     * 게시글 삭제
     */
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId,
                                           @AuthenticationPrincipal CustomUserDetails principal) {
        adminPostService.deletePost(postId, principal.getUsername());
        return ResponseEntity.noContent().build();
    }

}