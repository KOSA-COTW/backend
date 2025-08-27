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
import cotw.server.domain.board.repository.PostRepository;
import cotw.server.domain.member.entity.Member;
import cotw.server.domain.member.entity.Role;
import cotw.server.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
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

    private final AmazonS3Client s3Client;
    @Value("${aws.s3.bucket}")
    private String bucket;

    /**
     * 게시글 생성
     * - 기본 비공개(isPublic=false)
     * - 마감일은 오늘 이후
     * - 첫 번째 이미지 썸네일 지정
     */
    @Transactional
    public Long createPost(PostCreateRequestDto dto, String authorEmail) {

        // 작성자 정보 조회
        Member author = memberRepository.findByEmail(authorEmail)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 마감일 검증: 오늘 이후
        if (!dto.getDeadline().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("기부 마감일은 오늘 이후여야 합니다.");
        }

        // 엔티티 생성 및 이미지 바인딩
        Post post = dto.toPostEntity(author);
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

        // 1) 기존 이미지 삭제
        post.clearImages();

        // 2) 새 이미지 등록
        if (dto.getImageUrls() != null && !dto.getImageUrls().isEmpty()) {
            for (int i = 0; i < dto.getImageUrls().size(); i++) {
                Image img = Image.builder()
                        .post(post)
                        .imageUrl(dto.getImageUrls().get(i))
                        .isThumbnail(i == 0)   // 첫 번째 이미지를 썸네일로
                        .orderIndex(i)
                        .build();
                post.addImage(img);
            }
        }
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

    @Transactional(readOnly = true)
    public List<PostListResponseDto> getAdminOnlyPosts(
            Integer limit, 
            Integer page, 
            String sortDirection, 
            Category category) {
        
        // 기본값 설정
        int pageSize = (limit != null && (limit == 10 || limit == 20 || limit == 50)) ? limit : 10;
        int pageNumber = (page != null && page > 0) ? page - 1 : 0; // 0-based index
        String sort = (sortDirection != null && sortDirection.equalsIgnoreCase("ASC")) ? "ASC" : "DESC";
        
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        
        Page<Post> postsPage = postRepository.findAdminOnlyPosts(category, sort, pageable);
        
        return postsPage.getContent()
                .stream()
                .map(PostListResponseDto::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PostListResponseDto> getAdminOnlyPublicPosts(
            Integer limit, 
            Integer page, 
            String sortDirection, 
            Category category) {
        
        // 기본값 설정
        int pageSize = (limit != null && (limit == 10 || limit == 20 || limit == 50)) ? limit : 10;
        int pageNumber = (page != null && page > 0) ? page - 1 : 0; // 0-based index
        String sort = (sortDirection != null && sortDirection.equalsIgnoreCase("ASC")) ? "ASC" : "DESC";
        
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        
        Page<Post> postsPage = postRepository.findAdminOnlyPublicPosts(category, sort, pageable);
        
        return postsPage.getContent()
                .stream()
                .map(PostListResponseDto::new)
                .toList();
    }

    /**
     * 메인 화면용 6개
     * - 공개글만
     * - 마감 임박순 + 생성일 최신순
     * - 최대 6개
     */
    @Transactional(readOnly = true)
    public List<PostListResponseDto> getHomePosts() {
        var today = LocalDate.now();
        var top6 = postRepository.findHomePosts(today, PageRequest.of(0, 6));
        return top6.stream()
                .map(PostListResponseDto::new)
                .toList();
    }

    // 파일 업로드
    public String upload(MultipartFile image) throws IOException {
        // 원본 파일명
        String originalFileName = image.getOriginalFilename();

        // 저장할 파일명 (UUID 붙여 중복 방지)
        String fileName = changeFileName(originalFileName);

        // S3 메타데이터 생성
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(image.getContentType());
        metadata.setContentLength(image.getSize());

        // S3에 업로드
        s3Client.putObject(bucket, fileName, image.getInputStream(), metadata);

        // 업로드된 파일의 접근 URL 반환
        return s3Client.getUrl(bucket, fileName).toString();
    }

    // 파일명 변경 메서드 (UUID_원본파일명)
    private String changeFileName(String originalFileName) {
        return UUID.randomUUID().toString() + "_" + originalFileName;
    }
}
