package cotw.server.domain.board.dto.response;

import cotw.server.domain.board.entity.Notice;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class NoticeResponseDto {
    private Long id;
    private String title;
    private String content;
    private Boolean isPinned;
    private LocalDateTime createdAt;
    private List<String> imageUrls;

    public NoticeResponseDto(Notice notice) {
        this.id = notice.getId();
        this.title = notice.getTitle();
        this.content = notice.getContent();
        this.isPinned = notice.isPinned();
        this.createdAt = notice.getCreatedAt();
        this.imageUrls = notice.getImageUrls();
    }
}