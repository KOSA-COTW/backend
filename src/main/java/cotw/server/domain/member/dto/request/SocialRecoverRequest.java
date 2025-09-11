package cotw.server.domain.member.dto.request;

import cotw.server.domain.member.entity.ProviderType;

public record SocialRecoverRequest(
        String email,
        ProviderType provider
) { }
