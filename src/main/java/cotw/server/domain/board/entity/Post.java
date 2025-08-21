package cotw.server.domain.board.entity;

import cotw.server.common.BaseEntity;
import cotw.server.domain.board.dto.request.PostUpdateRequestDto;
import cotw.server.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 글 작성자 (단체 계정)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member author;

    // 카테고리
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    // 총 목표 금액
    @Column(nullable = false)
    private int amount;

    // 현재까지 모금된 금액
    @Column(nullable = false)
    private int currentAmount;

    // 공개 여부
    @Column(nullable = false)
    private boolean isPublic;

    // 기부 마감일
    @Column(nullable=false)
    private LocalDate deadline;

    // 이미지 리스트
    @Builder.Default
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Image> images = new ArrayList<>();

    // 기부한 사용자 목록
    @Builder.Default
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Participant> participants  = new ArrayList<>();

    // 댓글 목록
    @Builder.Default
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    public void addImage(Image image) {
        this.images.add(image);
        image.setPost(this);
    }

    public void update(PostUpdateRequestDto dto) {
        if (dto.getTitle() != null) this.title = dto.getTitle();
        if (dto.getContent() != null) this.content = dto.getContent();
        if (dto.getCategory() != null) this.category = dto.getCategory();
        if (dto.getAmount() > 0) this.amount = dto.getAmount();
    }

    // 공개 여부 변경
    public void changeVisibility(boolean isPublic) {
        this.isPublic = isPublic;
    }


    public boolean isCompleted() {
        return this.deadline.isBefore(LocalDate.now()); // 목표 달성 여부와 관계없이 마감일만으로 판단
    }

    public String getStatus() {
        return isCompleted() ? "COMPLETED" : "ONGOING";
    }

    // 기부자 수
    public int getDonorCount() {
        return this.participants != null ? this.participants.size() : 0;
    }
    
    // 기부 금액 추가
    public void addDonationAmount(int donationAmount) {
        if (donationAmount > 0) {
            this.currentAmount += donationAmount;
        }
    }
    
    // 기부 참여자 추가
    public void addParticipant(Participant participant) {
        this.participants.add(participant);

    }
}
