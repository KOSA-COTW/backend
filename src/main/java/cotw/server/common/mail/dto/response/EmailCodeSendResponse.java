package cotw.server.common.mail.dto.response;

public record EmailCodeSendResponse(boolean sent, String codeType, String message, int nextSendAfterSec) {
    public static EmailCodeSendResponse ok(int cooldown) {
        return new EmailCodeSendResponse(true, "OK", "인증코드를 전송했습니다.", cooldown);
    }
    public static EmailCodeSendResponse fail(String codeType, String message, int cooldown) {
        return new EmailCodeSendResponse(false, codeType, message, cooldown);
    }
}

