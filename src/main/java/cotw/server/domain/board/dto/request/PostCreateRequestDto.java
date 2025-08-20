package cotw.server.domain.board.dto.request;

import cotw.server.domain.board.entity.Category;
import cotw.server.domain.board.entity.Image;
import cotw.server.domain.board.entity.Post;
import cotw.server.domain.member.entity.Member;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
public class PostCreateRequestDto {

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    @NotNull(message = "카테고리를 선택하세요.")
    private Category category;

    // 총 목표 금액
    @Min(value = 1, message = "목표 금액은 1원 이상이어야 합니다.")
    private int amount;

    // 이미지 경로 리스트
    private List<String> imageUrls;

    @NotNull(message = "기부 마감일은 필수입니다.")
    private LocalDate deadline;

    // Post 엔티티로 변환
    public Post toPostEntity(Member author) {
        return Post.builder()
                .title(title)
                .content(content)
                .category(category)
                .amount(amount)
                .currentAmount(0)
                .author(author)
                .isPublic(false)
                .deadline(deadline)
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
