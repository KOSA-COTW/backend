package cotw.server.domain.admin.service;

import cotw.server.domain.admin.dto.request.AdminPostPageRequestDTO;
import cotw.server.domain.admin.dto.response.AdminPostCountResponseDTO;
import cotw.server.domain.admin.dto.response.AdminPostListResponseDto;
import cotw.server.domain.admin.dto.response.AdminPostPageResponseDTO;
import cotw.server.domain.board.entity.Post;
import cotw.server.domain.board.entity.PostVisibility;
import cotw.server.domain.board.repository.PostRepository;
import cotw.server.domain.member.entity.Member;
import cotw.server.domain.member.entity.Role;
import cotw.server.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminPostService {

    private final MemberRepository memberRepository;
    private final PostRepository postRepository;

    /**
     * 특정 상태(PostVisibility)에 해당하는 게시글 목록 조회
     * (예: PENDING 상태의 게시글만 가져오기)
     */
    @Transactional(readOnly = true)
    public List<AdminPostListResponseDto> getPostsByStatus(PostVisibility status) {
        List<Post> posts = postRepository.findAllByVisibilityStatus(status);
        return posts.stream()
                .map(AdminPostListResponseDto::new)
                .toList();
    }

    /**
     * 게시글 상태 변경 (승인, 반려, 비공개, 대기)
     * - 반려 시 rejectionReason을 함께 저장
     */
    @Transactional
    public void changePostsVisibility(List<Long> postIds, PostVisibility newStatus, String requesterEmail, String rejectionReason) {
        Member requester = memberRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        if (requester.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("관리자만 변경 가능합니다.");
        }

        var posts = postRepository.findAllById(postIds);
        for (Post post : posts) {
            switch (newStatus) {
                case APPROVED -> post.approve();
                case REJECTED -> post.reject(rejectionReason);
                case PRIVATE -> post.makePrivate();
                case PENDING -> post.markPending();
            }
        }
    }

    /**
     * 전체 게시글 목록 조회 (페이징, 필터링, 정렬)
     */
    @Transactional(readOnly = true)
    public AdminPostPageResponseDTO getAllPosts(AdminPostPageRequestDTO request) {
        request.validateAndSetDefaults();
        
        PageRequest pageRequest = PageRequest.of(request.getPage() - 1, request.getLimit());
        
        Page<Post> postPage = postRepository.findAllWithFilters(
                request.getVisibility() != null ? request.getVisibility().toString() : "",
                request.getCategory() != null ? request.getCategory().toString() : "",
                request.getTitle() != null ? request.getTitle() : "",
                request.getAuthorName() != null ? request.getAuthorName() : "",
                request.getSortBy(),
                request.getSortDirection(),
                pageRequest
        );
        
        List<AdminPostListResponseDto> postDtos = postPage.getContent().stream()
                .map(AdminPostListResponseDto::new)
                .toList();
        
        return new AdminPostPageResponseDTO(
                postDtos,
                postPage.getNumber() + 1,
                postPage.getTotalPages(),
                postPage.getTotalElements(),
                postPage.hasNext(),
                postPage.hasPrevious()
        );
    }

    /**
     * 게시글 삭제
     */
    @Transactional
    public void deletePost(Long postId, String requesterEmail) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        Member requester = memberRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        if (!post.getAuthor().getEmail().equals(requesterEmail) &&
                requester.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("관리자만 삭제할 수 있습니다.");
        }

        postRepository.delete(post);
    }

    /**
     * 게시글 개수 통계 조회
     */
    @Transactional(readOnly = true)
    public AdminPostCountResponseDTO getPostCounts() {
        long totalCount = postRepository.count();
        long privateCount = postRepository.countByVisibilityStatus(PostVisibility.PRIVATE);
        long pendingCount = postRepository.countByVisibilityStatus(PostVisibility.PENDING);
        long approvedCount = postRepository.countByVisibilityStatus(PostVisibility.APPROVED);
        long rejectedCount = postRepository.countByVisibilityStatus(PostVisibility.REJECTED);

        return new AdminPostCountResponseDTO(
                totalCount,
                privateCount,
                pendingCount,
                approvedCount,
                rejectedCount
        );
    }

}