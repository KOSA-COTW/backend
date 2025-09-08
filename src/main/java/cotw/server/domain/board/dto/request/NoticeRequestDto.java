package cotw.server.domain.board.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class NoticeRequestDto {

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 100, message = "제목은 100자 이하로 입력하세요.")
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    @Size(max = 5000, message = "내용은 5000자 이하로 입력하세요.")
    private String content;

    private Boolean isPinned;

    @Size(max = 5, message = "이미지는 최대 5개까지만 등록할 수 있습니다.")
    private List<String> imageUrls;
}