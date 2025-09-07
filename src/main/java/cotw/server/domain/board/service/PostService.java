package cotw.server.domain.board.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import cotw.server.domain.board.dto.request.PostCreateRequestDto;
import cotw.server.domain.board.dto.request.PostUpdateRequestDto;
import cotw.server.domain.board.dto.response.PostListResponseDto;
import cotw.server.domain.board.dto.response.PostResponseDto;
import cotw.server.domain.board.entity.Category;
import cotw.server.domain.board.entity.Image;
import cotw.server.domain.board.entity.Post;
import cotw.server.domain.board.entity.PostVisibility;
import cotw.server.domain.board.exception.BoardException;
import cotw.server.domain.board.exception.PostHasPaymentHistoryException;
import cotw.server.domain.board.repository.PostRepository;
import cotw.server.domain.member.entity.Member;
import cotw.server.domain.member.entity.Role;
import cotw.server.domain.member.repository.MemberRepository;
import cotw.server.domain.payment.repository.PaymentOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final AmazonS3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    /**
     * 게시글 생성
     * - 기본 상태: PRIVATE
     * - 마감일은 오늘 이후만 허용
     */
    @Transactional
    public Long createPost(PostCreateRequestDto dto, String authorEmail) {
        Member author = memberRepository.findByEmail(authorEmail)
                .orElseThrow(() -> new BoardException("존재하지 않는 회원입니다."));

        LocalDate today = LocalDate.now();

        // 마감일 검증
        if (dto.getDeadline().isBefore(today)) {
            throw new BoardException("기부 마감일은 오늘 이후여야 합니다.");
        }
        if (dto.getDeadline().isAfter(today.plusYears(1))) {
            throw new BoardException("기부 마감일은 1년 이내여야 합니다.");
        }

        // 금액 검증
        if (dto.getAmount() % 100 != 0) {
            throw new BoardException("목표 금액은 100원 단위로 입력해야 합니다.");
        }

        Post post = dto.toPostEntity(author);
        post.makePrivate(); // 무조건 비공개로 시작

        var images = dto.toImageEntityList(post);
        if (images != null) images.forEach(post::addImage);

        postRepository.save(post);
        return post.getId();
    }

    /**
     * 내 게시글 조회 (본인 글은 상태 상관없이 모두 보여줌)
     */
    @Transactional(readOnly = true)
    public List<PostResponseDto> getMyPosts(String email) {
        return postRepository.findAllByAuthor_Email(email)
                .stream().map(PostResponseDto::new).toList();
    }

    /**
     * 게시글 상세 조회
     * - APPROVED: 누구나 조회 가능
     * - 그 외: 작성자 본인 or ADMIN만 조회 가능
     */
    @Transactional(readOnly = true)
    public PostResponseDto getPostForView(Long postId, String viewerEmailOrNull) {
        Post post = postRepository.findDetailById(postId)
                .orElseThrow(() -> new BoardException("존재하지 않는 게시글입니다.", "POST_NOT_FOUND"));

        post.getParticipants().forEach(p -> p.getMember().getName());

        if (post.getVisibilityStatus() == PostVisibility.APPROVED) {
            return new PostResponseDto(post);
        }

        // 로그인 안 한 경우 차단
        if (viewerEmailOrNull == null) {
            throw new BoardException("비공개 게시글입니다.");
        }

        // 작성자 본인
        if (post.getAuthor().getEmail().equals(viewerEmailOrNull)) {
            return new PostResponseDto(post);
        }

        // 관리자
        Member viewer = memberRepository.findByEmail(viewerEmailOrNull)
                .orElseThrow(() -> new BoardException("존재하지 않는 회원입니다."));
        if (viewer.getRole() == Role.ADMIN) {
            return new PostResponseDto(post);
        }

        throw new BoardException("비공개 게시글입니다.");
    }

    /**
     * 게시글 수정
     * - 작성자 or 관리자 가능
     * - APPROVED 상태 글은 수정 시 PENDING으로 변경
     */
    @Transactional
    public void updatePost(Long postId, PostUpdateRequestDto dto, String editorEmail) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BoardException("존재하지 않는 게시글입니다."));

        Member requester = memberRepository.findByEmail(editorEmail)
                .orElseThrow(() -> new BoardException("존재하지 않는 회원입니다."));

        if (!post.getAuthor().getEmail().equals(editorEmail) &&
                requester.getRole() != Role.ADMIN) {
            throw new BoardException("수정 권한이 없습니다.");
        }

        if (dto.getDeadline().isBefore(LocalDate.now())) {
            throw new BoardException("기부 마감일은 오늘 이후여야 합니다.");
        }
        if (dto.getDeadline().isAfter(LocalDate.now().plusYears(1))) {
            throw new BoardException("기부 마감일은 1년 이내여야 합니다.");
        }
        if (dto.getAmount() % 100 != 0) {
            throw new BoardException("목표 금액은 100원 단위로 입력해야 합니다.");
        }

        post.update(dto); // 내부에서 APPROVED → PENDING 전환됨

        post.clearImages();
        if (dto.getImageUrls() != null) {
            for (int i = 0; i < dto.getImageUrls().size(); i++) {
                post.addImage(Image.builder()
                        .post(post)
                        .imageUrl(dto.getImageUrls().get(i))
                        .isThumbnail(i == 0)
                        .orderIndex(i)
                        .build());
            }
        }
    }

    /**
     * 게시글 삭제
     * - 작성자 or 관리자 가능
     * - 결제 내역이 있는 게시글은 삭제 불가
     */
    @Transactional
    public void deletePost(Long postId, String requesterEmail) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BoardException("존재하지 않는 게시글입니다."));

        Member requester = memberRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new BoardException("존재하지 않는 회원입니다."));

        if (!post.getAuthor().getEmail().equals(requesterEmail) &&
                requester.getRole() != Role.ADMIN) {
            throw new BoardException("삭제 권한이 없습니다.");
        }

        // 결제 내역이 있는지 확인
        if (paymentOrderRepository.existsByPostId(postId)) {
            throw new PostHasPaymentHistoryException("결제내역이 있는 게시물은 삭제할 수 없습니다.");
        }

        postRepository.delete(post);
    }

    /**
     * 홈화면용 글 6개 (승인된 글만)
     */
    @Transactional(readOnly = true)
    public List<PostListResponseDto> getHomePosts() {
        return postRepository.findHomePosts(LocalDate.now(), PageRequest.of(0, 6))
                .stream().map(PostListResponseDto::new).toList();
    }

    // 파일 업로드
    public String upload(MultipartFile image) throws IOException {
        String fileName = UUID.randomUUID() + "_" + image.getOriginalFilename();
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(image.getContentType());
        metadata.setContentLength(image.getSize());
        s3Client.putObject(bucket, fileName, image.getInputStream(), metadata);
        return s3Client.getUrl(bucket, fileName).toString();
    }

    /**
     * 유저가 관리자에게 승인 요청
     */
    @Transactional
    public void requestApproval(Long postId, Member user) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BoardException("존재하지 않는 게시글입니다."));
        if (!post.getAuthor().getId().equals(user.getId())) {
            throw new BoardException("본인 글만 승인 요청 가능");
        }
        if (post.getVisibilityStatus() == PostVisibility.PRIVATE ||
                post.getVisibilityStatus() == PostVisibility.REJECTED) {
            post.markPending(); // 승인 대기 상태로
        }
    }

    /**
     * 유저가 승인 요청 취소
     */
    @Transactional
    public void cancelApproval(Long postId, Member user) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BoardException("존재하지 않는 게시글입니다."));
        if (!post.getAuthor().getId().equals(user.getId())) {
            throw new BoardException("본인 글만 승인 요청 가능");
        }
        if (post.getVisibilityStatus() == PostVisibility.PENDING) {
            post.makePrivate(); // 다시 비공개 상태로
        }
    }

    @Transactional(readOnly = true)
    public List<PostListResponseDto> getPostsByStatus(PostVisibility status) {
        List<Post> posts = postRepository.findAllByVisibilityStatus(status);
        return posts.stream()
                .map(PostListResponseDto::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<PostListResponseDto> getApprovedPostsPaged(
            String category,
            String title,
            String authorName,
            String sortBy,
            String sortDirection,
            String fundStatus,
            int page,
            int size
    ) {
        Page<Post> posts = postRepository.findAllApprovedWithFilters(
                category, title, authorName,
                sortBy, sortDirection,
                fundStatus,
                PageRequest.of(page, size)
        );
        return posts.map(PostListResponseDto::new);
    }


}