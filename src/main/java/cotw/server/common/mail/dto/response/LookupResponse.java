package cotw.server.common.mail.dto.response;

public record LookupResponse(
        String email
) {
    public static LookupResponse from(String email) {
        return new LookupResponse(email);
    }
}
