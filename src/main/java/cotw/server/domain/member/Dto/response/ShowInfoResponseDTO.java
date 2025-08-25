package cotw.server.domain.member.Dto.response;

import cotw.server.domain.member.entity.Member;
import cotw.server.domain.member.entity.ProviderType;
import cotw.server.domain.member.entity.Role;

import java.time.LocalDateTime;

public record ShowInfoResponseDTO(
        Long memberId,
        String email,
        String name,
        String pictureUrl,
        Role role,
        ProviderType provider,
        LocalDateTime createAt,
        int oneTimeCount,
        Long totalDonation
) {

    public static ShowInfoResponseDTO from(Member member, int oneTimeCount, Long totalDonation) {
        return new ShowInfoResponseDTO(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getPictureUrl(),
                member.getRole(),
                member.getProvider(),
                member.getCreatedAt(),
                oneTimeCount,
                totalDonation
        );
    }
}
