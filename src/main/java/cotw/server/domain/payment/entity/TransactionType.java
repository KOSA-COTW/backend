package cotw.server.domain.payment.entity;

public enum TransactionType {
    PAYMENT_ATTEMPT,    // 결제 시도
    PAYMENT_CONFIRM,    // 결제 승인 확인
    PAYMENT_CANCEL,     // 결제 취소
    PAYMENT_REFUND,     // 환불 처리
    TOSS_API_CALL,      // 토스 API 호출
    SYSTEM_ERROR        // 시스템 에러
}