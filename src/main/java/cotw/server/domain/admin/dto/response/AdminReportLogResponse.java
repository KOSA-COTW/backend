package cotw.server.domain.admin.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record AdminReportLogResponse(
        Long id,
        String reporter,              // 신고자 이메일
        @JsonProperty("reason") String reason, // Enum → String 변환 값
        String detail,                // 기타 사유 상세
        LocalDateTime createdAt       // 신고 시간
) {}
