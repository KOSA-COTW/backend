package cotw.server.domain.member.dto.response;

public record DupCheckResponse(boolean available, String code, String message) {
    public static DupCheckResponse ok() { return new DupCheckResponse(true, "OK", null); }
    public static DupCheckResponse dup(String what) { return new DupCheckResponse(false, "DUPLICATE", what + " already used"); }
    public static DupCheckResponse invalid(String what) { return new DupCheckResponse(false, "INVALID_FORMAT", "Invalid " + what + " format"); }
}
