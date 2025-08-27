package cotw.server.domain.comment.entity;

public enum ReportReason {
    SPAM,            // 광고/도배/홍보
    ABUSE,           // 욕설/괴롭힘/혐오표현
    INAPPROPRIATE,   // 부적절/선정/NSFW
    PERSONAL_INFO,   // 개인정보 노출/도용
    ILLEGAL,         // 불법/범죄 조장
    ETC              // 기타(자유기입)
}
