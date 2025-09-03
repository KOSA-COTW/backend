package cotw.server.domain.admin.dto.request;

import cotw.server.domain.comment.entity.ReportReason;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CommentListRequest {
    private String keyword;                      // 내용/작성자/게시글 검색
    private String status = "ALL";               // ALL | PENDING | EXPIRED | HIDDEN
    private ReportReason reason;                 // SPAM | ABUSE | INAPPROPRIATE | PERSONAL_INFO | ILLEGAL | ETC
    Boolean reportedOnly;
    private boolean onlyPending = false;         // 미처리 우선
    private Integer minReports = 0;              // 최소 신고수
    private String sort = "REPORT_DESC";         // REPORT_DESC | DATE_DESC | DATE_ASC

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime start;                 // 기간 시작(옵션)

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime end;                   // 기간 종료(옵션)
}