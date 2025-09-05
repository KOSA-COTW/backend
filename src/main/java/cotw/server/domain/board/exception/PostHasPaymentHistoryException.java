package cotw.server.domain.board.exception;

public class PostHasPaymentHistoryException extends RuntimeException {
    public PostHasPaymentHistoryException(String message) {
        super(message);
    }
    
    public PostHasPaymentHistoryException() {
        super("결제내역이 있는 게시물은 삭제할 수 없습니다.");
    }
}