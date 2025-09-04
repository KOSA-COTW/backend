package cotw.server.domain.board.dto.response;

import cotw.server.domain.board.entity.Participant;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class DonorResponseDto {
    private Long memberId;
    private String name;
    private int amount;
    private String pictureUrl;
    private LocalDateTime createdAt;

    public DonorResponseDto(Participant participant) {
        this.memberId = participant.getMember().getId();
        this.name = participant.getMember().getName();
        this.amount = participant.getAmount();
        this.pictureUrl = participant.getMember().getPictureUrl();
        this.createdAt = participant.getCreatedAt();
    }
}
