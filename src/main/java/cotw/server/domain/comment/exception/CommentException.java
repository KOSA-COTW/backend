package cotw.server.domain.comment.exception;

import lombok.Getter;

@Getter
public class CommentException extends RuntimeException {
    private final String errorCode;

    public CommentException(String message) {
        super(message);
        this.errorCode = "COMMENT_ERROR";
    }

    public CommentException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}