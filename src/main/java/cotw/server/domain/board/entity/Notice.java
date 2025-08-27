package cotw.server.domain.board.entity;

import cotw.server.common.BaseEntity;
import cotw.server.domain.board.dto.request.NoticeRequestDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notice extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private boolean isPinned; // 상단 고정 여부

    // 이미지 URL 리스트
    @ElementCollection
    @CollectionTable(name = "notice_images", joinColumns = @JoinColumn(name = "notice_id"))
    @Column(name = "image_url")
    private List<String> imageUrls = new ArrayList<>();

    public void update(NoticeRequestDto dto) {
        if (dto.getTitle() != null) {
            this.title = dto.getTitle();
        }
        if (dto.getContent() != null) {
            this.content = dto.getContent();
        }
        if (dto.getIsPinned() != null) {
            this.isPinned = dto.getIsPinned();
        }
        if (dto.getImageUrls() != null) {
            this.imageUrls.clear();
            this.imageUrls.addAll(dto.getImageUrls());
        }
    }
}
