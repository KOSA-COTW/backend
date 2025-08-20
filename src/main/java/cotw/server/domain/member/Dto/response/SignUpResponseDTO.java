package cotw.server.domain.member.Dto.response;

import cotw.server.domain.member.entity.Member;

public record SignUpResponseDTO(
        String name,
        String email,
        String password) {

    public static SignUpResponseDTO fromEntity(Member member) {
        return new SignUpResponseDTO(
                member.getName(),
                member.getEmail(),
                member.getPassword()
        );
    }
}
