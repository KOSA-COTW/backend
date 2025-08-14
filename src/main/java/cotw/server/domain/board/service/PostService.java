package cotw.server.domain.board.service;

import cotw.server.domain.board.dto.request.PostCreateRequestDto;
import cotw.server.domain.board.dto.request.PostUpdateRequestDto;
import cotw.server.domain.board.dto.response.PostListResponseDto;
import cotw.server.domain.board.dto.response.PostResponseDto;
import cotw.server.domain.board.entity.Image;
import cotw.server.domain.board.entity.Post;
import cotw.server.domain.board.repository.PostRepository;
import cotw.server.domain.member.entity.Member;
import cotw.server.domain.member.entity.Role;
import cotw.server.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;

    /**
     * 게시글 생성
     * - 로그인한 사용자만 가능
     * - 기본적으로 isPublic = false로 저장 (Post 엔티티에서 기본값 설정)
     */
    @Transactional
    public Long createPost(PostCreateRequestDto dto, String authorEmail) {

        // 작성자 정보 조회
        Member author = memberRepository.findByEmail(authorEmail)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // Post 엔티티 생성
        Post post = dto.toPostEntity(author);

        // 이미지 엔티티 리스트 생성 & Post와 연관관계 설정
        List<Image> images = dto.toImageEntityList(post);
        images.forEach(post::addImage);

        // DB 저장
        postRepository.save(post);
        return post.getId();
    }

    /**
     * 나의 게시글 전체 조회
     * - 로그인한 사용자의 이메일 기준
     * - 공개/비공개 상관없이 본인이 작성한 모든 글
     */
    @Transactional(readOnly = true)
    public List<PostResponseDto> getMyPosts(String email) {
        return postRepository.findAllByAuthor_Email(email)
                .stream().map(PostResponseDto::new).toList();
    }

    /**
     * 특정 게시글 조회
     * - 공개글: 누구나 가능
     * - 비공개글: 작성자 본인 또는 관리자만 가능
     */
    @Transactional(readOnly = true)
    public PostResponseDto getPostForView(Long postId, String viewerEmailOrNull) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        if (post.isPublic()) {
            return new PostResponseDto(post);
        }

        // 비공개: 로그인 사용자만 검사
        if (viewerEmailOrNull == null) {
            throw new AccessDeniedException("비공개 게시글입니다.");
        }

        // 작성자 or ADMIN 허용
        if (post.getAuthor().getEmail().equals(viewerEmailOrNull)) {
            return new PostResponseDto(post);
        }
        Member viewer = memberRepository.findByEmail(viewerEmailOrNull)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        if (viewer.getRole() == Role.ADMIN) {
            return new PostResponseDto(post);
        }

        throw new AccessDeniedException("비공개 게시글입니다.");
    }

    /**
     * 게시글 삭제
     * - 작성자이거나 관리자일 경우 삭제 가능
     */
    @Transactional
    public void deletePost(Long postId, String authorEmail) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        Member requester = memberRepository.findByEmail(authorEmail)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 작성자이거나 관리자일 경우 삭제 허용
        if (!post.getAuthor().getEmail().equals(authorEmail) &&
                !requester.getRole().equals(Role.ADMIN)) {
            throw new AccessDeniedException("삭제 권한이 없습니다.");
        }

        postRepository.delete(post);
    }

    /**
     * 게시글 수정
     * - 작성자이거나 관리자일 경우 수정 가능
     */
    @Transactional
    public void updatePost(Long postId, PostUpdateRequestDto dto, String editorEmail) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        Member requester = memberRepository.findByEmail(editorEmail)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 작성자이거나 관리자일 경우 수정 허용
        if (!post.getAuthor().getEmail().equals(editorEmail) &&
                !requester.getRole().equals(Role.ADMIN)) {
            throw new AccessDeniedException("수정 권한이 없습니다.");
        }

        post.update(dto);
    }

    /**
     * 게시글 공개 여부 변경
     * - 관리자만 가능
     * - isPublic = true → 공개, false → 비공개
     */
    @Transactional
    public void changePostVisibility(Long postId, boolean isPublic, String requesterEmail) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        Member requester = memberRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 권한 검증 (관리자만)
        if (!requester.getRole().equals(Role.ADMIN)) {
            throw new AccessDeniedException("관리자만 변경 가능합니다.");
        }

        post.changeVisibility(isPublic);
    }

    /**
     * 공개 상태의 모든 게시글 조회
     * - isPublic = true 조건에 해당하는 게시글만 DB에서 조회
     */
    @Transactional(readOnly = true)
    public List<PostListResponseDto> getAllPublicPosts() {
        // 공개 글만 조회
        List<Post> posts = postRepository.findAllByIsPublicTrue();

        return posts.stream()
                .map(PostListResponseDto::new)
                .toList();
    }
}
