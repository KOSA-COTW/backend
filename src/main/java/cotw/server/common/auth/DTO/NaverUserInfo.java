package cotw.server.common.auth.DTO;


import cotw.server.common.auth.OAuth2UserInfo;

import java.util.Map;

public record NaverUserInfo(String id, String email, String name, String picture) implements OAuth2UserInfo {
    public static NaverUserInfo from(Map<String, Object> attr) {
        Map<String, Object> resp = OAuth2UserInfo.safeMap(attr.get("response"));
        return new NaverUserInfo(
                (String) resp.get("id"),
                (String) resp.get("email"),
                (String) resp.get("name"),
                (String) resp.get("profile_image")
        );
    }

    @Override
    public Map<String, Object> toAttributeMap(){
        return Map.of("provider", "naver", "id", id, "email", email, "name", name, "picture", picture);
    }
}
