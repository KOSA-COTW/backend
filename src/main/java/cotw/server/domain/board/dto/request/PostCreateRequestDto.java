package cotw.server.domain.board.dto.request;

import cotw.server.domain.board.entity.Category;
import cotw.server.domain.board.entity.Image;
import cotw.server.domain.board.entity.Post;
import cotw.server.domain.member.entity.Member;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
public class PostCreateRequestDto {

    // [임시 필드] 로그인 기능이 없어 사용자 식별을 위해 memberId를 직접 받음.
    // 추후 인증 기능 연동 시 제거 예정
    private Long memberId;

    private String title;

    private String content;

    private Category category;

    // 총 목표 금액
    private int amount;

    // 업로드된 이미지 경로 리스트
    private List<String> imageUrls;

    // Post 엔티티로 변환
    public Post toPostEntity(Member author) {
        return Post.builder()
                .title(title)
                .content(content)
                .category(category)
                .amount(amount)
                .currentAmount(0)
                .author(author)
                .build();
    }

    // 이미지 URL들을 Image 엔티티 리스트로 변환
    public List<Image> toImageEntityList(Post post) {
        List<Image> images = new ArrayList<>();
        if (imageUrls != null) {
            for (int i = 0; i < imageUrls.size(); i++) {
                images.add(Image.builder()
                        .imageUrl(imageUrls.get(i))
                        .isThumbnail(i == 0) // 첫 번째 이미지를 썸네일로 설정
                        .orderIndex(i)
                        .post(post)
                        .build());
            }
        }
        return images;
    }

}
