package cotw.server.domain.payment.entity;

public enum PaymentStatus {
    READY,         // 인증 전
    IN_PROGRESS,   // 인증
    DONE,          // 결제 승인
    CANCELED,      // 결제 취소
    ABORTED,       // 결제 실패
    EXPIRED        // 유효 시간 초과
}
