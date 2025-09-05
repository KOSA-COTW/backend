package cotw.server.domain.board.dto.request;

import cotw.server.domain.board.entity.Category;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
public class PostUpdateRequestDto {
    private String title;
    private String content;
    private Category category;
    @Min(value = 100, message = "목표 금액은 100원 이상이어야 합니다.")
    @Max(value = 1_000_000_000L, message = "목표 금액은 10억 원 이하만 가능합니다.")
    private long amount;
    @Size(max = 5, message = "이미지는 최대 5장까지 업로드할 수 있습니다.")
    private List<String> imageUrls;
    private LocalDate deadline;
}