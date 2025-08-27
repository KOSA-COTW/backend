package cotw.server.domain.payment.entity;

public enum TransactionResult {
    SUCCESS,    // 성공
    FAILURE,    // 실패
    PENDING,    // 대기중
    TIMEOUT,    // 타임아웃
    ERROR       // 에러
}