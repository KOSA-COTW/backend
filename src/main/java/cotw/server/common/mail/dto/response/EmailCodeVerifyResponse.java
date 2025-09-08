package cotw.server.common.mail.dto.response;

public record EmailCodeVerifyResponse(boolean verified, String codeType, String message) {
    public static EmailCodeVerifyResponse ok() { return new EmailCodeVerifyResponse(true, "OK", "인증이 완료되었습니다."); }
    public static EmailCodeVerifyResponse fail(String codeType, String message) { return new EmailCodeVerifyResponse(false, codeType, message); }
}
