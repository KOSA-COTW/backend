package cotw.server.domain.board.service;

import cotw.server.domain.board.dto.request.PostCreateRequestDto;
import cotw.server.domain.board.dto.request.PostUpdateRequestDto;
import cotw.server.domain.board.dto.response.PostResponseDto;
import cotw.server.domain.board.entity.Image;
import cotw.server.domain.board.entity.Post;
import cotw.server.domain.board.repository.PostRepository;
import cotw.server.domain.member.entity.Member;
import cotw.server.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;

    // 게시글 생성
    @Transactional
    public Long createPost(PostCreateRequestDto dto) {
        // [임시 구현] 현재 로그인 기능이 없기 때문에 memberId를 DTO로 받아서 조회함.
        // 로그인 기능 구현 후에는 @AuthenticationPrincipal을 통해 인증된 Member를 주입받고,
        // DTO에서 memberId 필드를 제거하는 방식으로 리팩토링할 예정.
        Member author = memberRepository.findById(dto.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // Post 엔티티 생성
        Post post = dto.toPostEntity(author);

        // 이미지 엔티티 리스트 생성 및 연관관계 설정
        List<Image> images = dto.toImageEntityList(post);
        images.forEach(post::addImage);

        postRepository.save(post);
        return post.getId();
    }

    // 전체 게시글 목록 조회
    @Transactional(readOnly = true)
    public List<PostResponseDto> getAllPosts() {
        List<Post> posts = postRepository.findAll();
        List<PostResponseDto> dtoList = new ArrayList<>();

        for (Post post : posts) {
            dtoList.add(new PostResponseDto(post));
        }

        return dtoList;
    }

    // 특정 게시글 조회
    public PostResponseDto getPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(()->new IllegalArgumentException("존재하지 않는 게시글입니다."));
        return new PostResponseDto(post);
    }

    // 게시글 삭제
    @Transactional
    public void deletePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        postRepository.delete(post);
    }

    // 게시글 수정
    @Transactional
    public void updatePost(Long postId, PostUpdateRequestDto dto) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        post.update(dto);
    }
}
