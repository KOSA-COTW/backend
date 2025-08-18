package cotw.server.domain.board.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import cotw.server.domain.board.entity.Category;
import cotw.server.domain.board.entity.Image;
import cotw.server.domain.board.entity.Post;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
public class PostResponseDto {

    private Long id;
    private String title;
    private String content;
    private String authorName;
    private Category category;
    private int amount;
    private int currentAmount;
    private LocalDate deadline;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime updatedAt;
    private List<String> imageUrls;

    public PostResponseDto(Post post) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.content = post.getContent();
        this.authorName = post.getAuthor().getName();
        this.category = post.getCategory();
        this.amount = post.getAmount();
        this.currentAmount = post.getCurrentAmount();
        this.deadline = post.getDeadline();
        this.createdAt = post.getCreatedAt();
        this.updatedAt = post.getUpdatedAt();
        List<String> imageUrls = new ArrayList<>();
        for (Image image : post.getImages()) {
            imageUrls.add(image.getImageUrl());
        }
        this.imageUrls = imageUrls;
    }
}
