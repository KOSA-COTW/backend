package cotw.server.domain.board.domain;

import cotw.server.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Image extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 이미지 경로
    @Column(nullable = false)
    private String imageUrl;

    // 대표 이미지 여부
    @Column(nullable = false)
    private boolean isThumbnail = false;

    // 정렬 순서
    private int orderIndex;

    // 어떤 게시글(Post)에 속한 이미지인지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;
}
