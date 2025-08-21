package cotw.server.domain.board.dto.response;

import cotw.server.domain.board.entity.Post;
import lombok.Getter;

@Getter
public class PostListResponseDto {
    private Long id;
    private String title;
    private String category; // 예: "아동", "장애인" (displayName)
    private int target;      // = amount
    private int raised;      // = currentAmount
    private double percent;  // = raised / target * 100
    private int remaining;   // = target - raised
    private String image;    // 대표 이미지 URL(없으면 null)

    // DB 매핑용 생성자
    public PostListResponseDto(Post post) {
        this.id = post.getId();
        this.title = post.getTitle();
        // enum → 한글 표시명으로
        this.category = post.getCategory().getDisplayName();
        this.target = post.getAmount();
        this.raised = post.getCurrentAmount();
        this.remaining = target - raised;
        this.percent = (target > 0) ? (raised / (double) target) * 100 : 0.0;
        this.image = post.getImages().isEmpty() ? null : post.getImages().get(0).getImageUrl();
    }

}
