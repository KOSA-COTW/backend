package cotw.server.domain.comment.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class CommentExceptionHandler {

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message, String errorCode) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("errorCode", errorCode);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(CommentException.class)
    public ResponseEntity<Map<String, Object>> handleCommentException(CommentException e) {
        log.warn("Comment exception occurred: {} ({})", e.getMessage(), e.getErrorCode());

        HttpStatus status;
        // 프론트 분기(409/429/400/404/403)에 맞춘 매핑
        switch (String.valueOf(e.getErrorCode())) {
            case "COMMENT_NOT_FOUND" -> status = HttpStatus.NOT_FOUND;          // 404
            case "COMMENT_ACCESS_DENIED" -> status = HttpStatus.FORBIDDEN;      // 403
            case "REPORT_DAILY_LIMIT" -> status = HttpStatus.TOO_MANY_REQUESTS; // 429
            case "ALREADY_REPORTED" -> status = HttpStatus.CONFLICT;            // 409

            // 프론트에서 400으로 처리하는 케이스들
            case "COMMENT_DELETED", "COMMENT_HIDDEN",
                 "REPORT_DETAIL_REQUIRED",
                 "CANNOT_REPORT_OWN_COMMENT" -> status = HttpStatus.BAD_REQUEST; // 400

            default -> status = HttpStatus.BAD_REQUEST;                          // 400
        }

        return build(status, e.getMessage(), e.getErrorCode());
    }

    /**
     * Bean Validation (@Valid) 실패 처리
     * - ReportRequest(reason/detail) 검증 실패 시 400 반환
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getDefaultMessage())
                .orElse("잘못된 요청입니다.");
        return build(HttpStatus.BAD_REQUEST, msg, "VALIDATION_ERROR");
    }
}
