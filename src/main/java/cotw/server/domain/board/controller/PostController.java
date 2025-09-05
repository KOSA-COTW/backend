package cotw.server.domain.board.controller;

import cotw.server.common.jwt.CustomUserDetails;
import cotw.server.domain.board.dto.request.PostCreateRequestDto;
import cotw.server.domain.board.dto.request.PostUpdateRequestDto;
import cotw.server.domain.board.dto.response.PostListResponseDto;
import cotw.server.domain.board.dto.response.PostResponseDto;
import cotw.server.domain.board.entity.PostVisibility;
import cotw.server.domain.board.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
     * - 기본 상태: PRIVATE
     */
    @PostMapping
    public ResponseEntity<?> createPost(@AuthenticationPrincipal CustomUserDetails principal,
                                        @Valid @RequestBody PostCreateRequestDto dto) {
        Long postId = postService.createPost(dto, principal.getUsername());
        return ResponseEntity.ok(postId);
    }

    /**
     * 내가 쓴 모든 게시글 조회
     * - 본인 글은 PRIVATE, PENDING, APPROVED, REJECTED 전부 조회 가능
     */
    @GetMapping("/me") //todo 마이페이지
    public ResponseEntity<List<PostResponseDto>> getMyPosts(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(postService.getMyPosts(principal.getUsername()));
    }

    /**
     * 특정 게시글 조회
     * - APPROVED: 누구나 조회 가능
     * - PRIVATE/PENDING/REJECTED: 작성자 본인 or 관리자만 조회 가능
     */
    @GetMapping("/{postId}")
    public ResponseEntity<PostResponseDto> getPost(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails principal // null 가능
    ) {
        String viewerEmail = (principal == null) ? null : principal.getUsername();
        return ResponseEntity.ok(postService.getPostForView(postId, viewerEmail));
    }

    /**
     * 게시글 삭제
     * - 작성자 본인 or 관리자 가능
     */
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId,
                                           @AuthenticationPrincipal CustomUserDetails principal) {
        postService.deletePost(postId, principal.getUsername());
        return ResponseEntity.noContent().build();
    }

    /**
     * 게시글 수정
     * - 작성자 본인 or 관리자 가능
     * - APPROVED 상태에서 수정 시 PENDING으로 변경
     */
    @PatchMapping("/{postId}")
    public ResponseEntity<Void> updatePost(@AuthenticationPrincipal CustomUserDetails principal,
                                           @PathVariable Long postId,
                                           @RequestBody PostUpdateRequestDto dto) {
        postService.updatePost(postId, dto, principal.getUsername());
        return ResponseEntity.ok().build();
    }

    /**
     * 승인된 게시글 목록 조회
     * - 누구나 접근 가능
     */
    @GetMapping
    public ResponseEntity<List<PostListResponseDto>> getApprovedPosts() {
        return ResponseEntity.ok(postService.getPostsByStatus(PostVisibility.APPROVED));
    }

    /**
     * 메인 화면용: 승인된 글 중 마감 임박 6개
     */
    @GetMapping("/home")
    public ResponseEntity<List<PostListResponseDto>> getHomePosts() {
        return ResponseEntity.ok(postService.getHomePosts());
    }

    /**
     * 이미지 업로드
     */
    @PostMapping("/upload")
    public Map<String, String> uploadImage(@RequestPart("file") MultipartFile file) throws IOException {
        String imageUrl = postService.upload(file);
        return Map.of("url", imageUrl);
    }

    // 승인 요청
    @PostMapping("/{postId}/request-approval")
    public ResponseEntity<Void> requestApproval(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        postService.requestApproval(postId, principal.getMember());
        return ResponseEntity.ok().build();
    }

    // 승인 요청 취소
    @PostMapping("/{postId}/cancel-approval")
    public ResponseEntity<Void> cancelApproval(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        postService.cancelApproval(postId, principal.getMember());
        return ResponseEntity.ok().build();
    }

}