package cotw.server.domain.member.dto.request;

public record PatchPasswordRequestDTO(
        String currentPassword,
        String newPassword
) {
}
