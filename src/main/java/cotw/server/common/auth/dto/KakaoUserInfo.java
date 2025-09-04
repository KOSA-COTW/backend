package cotw.server.common.auth.dto;

import cotw.server.common.auth.OAuth2UserInfo;

import java.util.Map;

public record KakaoUserInfo(String id, String email, String name, String picture) implements OAuth2UserInfo {
    public static KakaoUserInfo from(Map<String, Object> attr) {
        String id = String.valueOf(attr.get("id"));
        Map<String, Object> account = OAuth2UserInfo.safeMap(attr.get("kakao_account"));
        Map<String, Object> profile = account != null ? OAuth2UserInfo.safeMap(account.get("profile")) : Map.of();
        return new KakaoUserInfo(
                id,
                account != null ? (String) account.get("email") : null,
                profile != null ? (String) profile.get("nickname") : null,
                profile != null ? (String) profile.get("profile_image_url") : null
        );
    }

    @Override
    public Map<String, Object> toAttributeMap(){ return Map.of(
            "provider", "kakao", "id", id, "email", email, "name", name, "picture", picture
    );}
}
