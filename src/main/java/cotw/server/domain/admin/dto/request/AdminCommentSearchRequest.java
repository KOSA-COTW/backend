// src/main/java/cotw/server/domain/admin/dto/request/AdminCommentSearchRequest.java
package cotw.server.domain.admin.dto.request;

import lombok.Data;

@Data
public class AdminCommentSearchRequest {
    private String status = "ALL";       // ALL | REPORTED | EXPIRED | HIDDEN
    private String reason;               // SPAM | ABUSE | ...
    private Boolean reportedOnly;        // true면 신고있는 것만
    private Boolean onlyPending;         // true면 검토기한 남은 숨김만
    private String keyword;              // 본문/작성자/게시글 검색
    private Integer minReports = 0;      // 최소 신고수
    private String sort = "REPORT_DESC"; // REPORT_DESC | DATE_DESC | DATE_ASC
    private String from;                 // ISO-8601 (yyyy-MM-ddTHH:mm:ss) or date
    private String to;                   // ISO-8601
    private Integer page = 1;            // 1-base
    private Integer size = 10;
}
