package cotw.server.domain.board.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class NoticeRequestDto {
    private String title;
    private String content;
    private Boolean isPinned;
    private List<String> imageUrls;
}
