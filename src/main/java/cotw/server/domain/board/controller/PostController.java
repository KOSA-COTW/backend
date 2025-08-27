package cotw.server.domain.board.controller;

import cotw.server.common.jwt.CustomUserDetails;
import cotw.server.domain.board.dto.request.PostCreateRequestDto;
import cotw.server.domain.board.dto.request.PostUpdateRequestDto;
import cotw.server.domain.board.dto.response.PostListResponseDto;
import cotw.server.domain.board.dto.response.PostResponseDto;
import cotw.server.domain.board.entity.Category;
import cotw.server.domain.board.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /**
     * 게시글 생성
     * - 로그인한 사용자만 가능
     * - 작성 시 기본 isPublic = false (비공개)
     */
    @PostMapping
    public ResponseEntity<?> createPost(@AuthenticationPrincipal CustomUserDetails principal,
                                        @Valid @RequestBody PostCreateRequestDto dto) {
        Long postId = postService.createPost(dto, principal.getUsername());
        return ResponseEntity.ok(postId);
    }

    /**
     * 내가 쓴 모든 게시글 목록 조회
     * - 비공개/공개 상관없이 본인이 작성한 모든 글 조회
     */
    @GetMapping("/me")
    public ResponseEntity<List<PostResponseDto>> getMyPosts(
            @AuthenticationPrincipal CustomUserDetails principal) {
        List<PostResponseDto> posts = postService.getMyPosts(principal.getUsername());
        return ResponseEntity.ok(posts);
    }

    /**
     * 특정 게시글 조회
     * - 공개글: 누구나 조회 가능
     * - 비공개글: 작성자 본인 or 관리자만 조회 가능
     */
    @GetMapping("/{postId}")
    public ResponseEntity<PostResponseDto> getPost(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails principal // null일 수 있음
    ) {
        String viewerEmail = (principal == null) ? null : principal.getUsername();
        return ResponseEntity.ok(postService.getPostForView(postId, viewerEmail));
    }

    /**
     * 게시글 삭제
     * - 작성자 본인 or 관리자만 가능
     */
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId,
                                           @AuthenticationPrincipal CustomUserDetails principal) {
        postService.deletePost(postId, principal.getUsername());
        return ResponseEntity.noContent().build();
    }

    /**
     * 게시글 수정
     * - 작성자 본인 or 관리자만 가능
     */
    @PatchMapping("/{postId}")
    public ResponseEntity<Void> updatePost(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long postId,
                                           @RequestBody PostUpdateRequestDto dto) {
        postService.updatePost(postId, dto, principal.getUsername());
        return ResponseEntity.ok().build();
    }

    /**
     * 게시글 공개 여부 변경
     * - 관리자만 가능
     * - 작성 시 기본 false → 관리자가 true로 변경하면 공개됨
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{postId}/visibility")
    public ResponseEntity<Void> changePostVisibility(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long postId,
            @RequestParam boolean isPublic) {
        postService.changePostVisibility(postId, isPublic, principal.getUsername());
        return ResponseEntity.ok().build();
    }

    /**
     * 공개된 모든 게시글 목록 조회
     * - 로그인 여부와 관계없이 접근 가능
     * - 비공개 게시글은 제외하고, isPublic = true 인 게시글만 반환
     */
    @GetMapping
    public ResponseEntity<List<PostListResponseDto>> getAllPublicPosts() {
        List<PostListResponseDto> posts = postService.getAllPublicPosts();
        return ResponseEntity.ok(posts);
    }

    /**
     * 관리자용: 비공개 게시글 조회
     * - 관리자만 접근 가능
     * - 페이징, 정렬, 카테고리 필터링 지원
     */
    @GetMapping("/admin")
    public ResponseEntity<List<PostListResponseDto>> getAdminOnlyPosts(
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @RequestParam(required = false) Category category) {
        
        List<PostListResponseDto> posts = postService.getAdminOnlyPosts(limit, page, sortDirection, category);
        return ResponseEntity.ok(posts);
    }

    /**
     * 관리자용: 공개 게시글 조회
     * - 관리자만 접근 가능
     * - 페이징, 정렬, 카테고리 필터링 지원
     * - isPublic = true인 게시글만 조회
     */
    @GetMapping("/admin/public")
    public ResponseEntity<List<PostListResponseDto>> getAdminOnlyPublicPosts(
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @RequestParam(required = false) Category category) {
        
        List<PostListResponseDto> posts = postService.getAdminOnlyPublicPosts(limit, page, sortDirection, category);
        return ResponseEntity.ok(posts);
    }

    /**
     * 메인 화면용: 공개 + 마감 임박 6개
     * - 누구나 접근 가능
     */
    @GetMapping("/home")
    public ResponseEntity<List<PostListResponseDto>> getHomePosts() {
        return ResponseEntity.ok(postService.getHomePosts());
    }

    @PostMapping("/upload")
    public Map<String, String> upload(@RequestPart("file") MultipartFile file) throws IOException {
        String imageUrl = postService.upload(file);
        return Map.of("url", imageUrl);
    }
}
