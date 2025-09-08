package cotw.server.domain.member.dto.request;

public record PatchPasswordRequest(
        String currentPassword,
        String newPassword
) {
}
