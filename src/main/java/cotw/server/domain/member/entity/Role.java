package cotw.server.domain.member.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role {
    ADMIN("관리자"), USER("일반유저");

    private final String description;

    public String getAuthority() {
        return "ROLE_" + this.name();
    }
}
