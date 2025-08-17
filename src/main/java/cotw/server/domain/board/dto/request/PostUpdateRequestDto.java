package cotw.server.domain.board.dto.request;

import cotw.server.domain.board.entity.Category;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PostUpdateRequestDto {
    private String title;
    private String content;
    private Category category;
    private int amount;
}
