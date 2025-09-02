package cotw.server.domain.board.dto.request;

import cotw.server.domain.board.entity.Category;
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
    private int amount;
    private List<String> imageUrls;
    private LocalDate deadline;
}