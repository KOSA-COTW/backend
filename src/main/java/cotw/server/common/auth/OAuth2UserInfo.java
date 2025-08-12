package cotw.server.common.auth;

import java.util.Map;

public interface OAuth2UserInfo {
    String id();
    String email();
    String name();
    String picture();
    Map<String, Object> toAttributeMap();

    @SuppressWarnings("unchecked")
    static Map<String, Object> safeMap(Object obj) {
        return (obj instanceof Map<?, ?> m) ? (Map<String, Object>) m : Map.of();
    }
}



