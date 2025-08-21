package cotw.server.domain.member.Dto.response;

import cotw.server.domain.member.entity.Member;
import cotw.server.domain.member.entity.ProviderType;
import cotw.server.domain.member.entity.Role;

import java.time.LocalDateTime;

public record ShowInfoResponseDTO(
        String email,
        String name,
        String pictureUrl,
        Role role,
        ProviderType provider,
        LocalDateTime createAt
) {

    public static ShowInfoResponseDTO from(Member member) {
        return new ShowInfoResponseDTO(
                member.getEmail(),
                member.getName(),
                member.getPictureUrl(),
                member.getRole(),
                member.getProvider(),
                member.getCreatedAt()
        );
    }
}
