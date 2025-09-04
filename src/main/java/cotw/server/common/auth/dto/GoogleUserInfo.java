package cotw.server.common.auth.dto;

import cotw.server.common.auth.OAuth2UserInfo;

import java.util.Map;

public record GoogleUserInfo(String id, String email, String name, String picture) implements OAuth2UserInfo {
    public static GoogleUserInfo from(Map<String, Object> attr) {
        return new GoogleUserInfo(
                (String) attr.get("sub"),
                (String) attr.get("email"),
                (String) attr.get("name"),
                (String) attr.get("picture")
        );
    }

    @Override
    public Map<String, Object> toAttributeMap() {
        return Map.of("provider","google","id", id, "email", email, "name", name, "picture", picture);
    }
}
