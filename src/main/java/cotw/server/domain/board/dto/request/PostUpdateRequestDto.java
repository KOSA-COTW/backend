package cotw.server.domain.board.dto.request;

import cotw.server.domain.board.entity.Category;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
public class PostUpdateRequestDto {
    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 100, message = "제목은 100자 이하로 입력하세요.")
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    @Size(min = 10, max = 5000, message = "내용은 10자 이상 5000자 이하로 입력하세요.")
    private String content;

    @NotNull(message = "카테고리를 선택하세요.")
    private Category category;

    @Min(value = 100, message = "목표 금액은 100원 이상이어야 합니다.")
    @Max(value = 1_000_000_000L, message = "목표 금액은 10억 원 이하만 가능합니다.")
    private long amount;

    @Size(max = 5, message = "이미지는 최대 5장까지 업로드할 수 있습니다.")
    private List<String> imageUrls;

    @NotNull(message = "기부 마감일은 필수입니다.")
    @FutureOrPresent(message = "마감일은 오늘 이후여야 합니다.")
    private LocalDate deadline;
}