package cotw.server.domain.board.entity;

public enum PostVisibility {
    PRIVATE,   // 기본값 (유저 작성 시 비공개)
    PENDING,   // 관리자 승인 대기
    APPROVED,  // 관리자 승인 완료 → 공개
    REJECTED   // 관리자 반려
}