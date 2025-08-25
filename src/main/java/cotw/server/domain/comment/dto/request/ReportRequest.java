package cotw.server.domain.comment.dto.request;

import cotw.server.domain.comment.entity.ReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 신고 생성 요청 DTO
 * - reason: 신고 사유(필수)
 * - detail: 기타(ETC) 선택 시 상세(선택, 200자 제한)
 */
public record ReportRequest(
        @NotNull(message = "신고 사유를 선택해주세요")
        ReportReason reason,

        @Size(max = 200, message = "상세 내용은 200자 이하로 입력해주세요")
        String detail
) {}
