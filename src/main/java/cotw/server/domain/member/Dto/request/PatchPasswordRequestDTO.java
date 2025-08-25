package cotw.server.domain.member.Dto.request;

public record PatchPasswordRequestDTO(
        String currentPassword,
        String newPassword
) {
}
