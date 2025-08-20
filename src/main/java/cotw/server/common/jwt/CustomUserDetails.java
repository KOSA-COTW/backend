package cotw.server.common.jwt;

import cotw.server.domain.member.entity.Member;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.*;

public class CustomUserDetails implements UserDetails, OAuth2User {

    private final Member member;
    private final Map<String, Object> attributes; // OAuth2 attributes (일반 로그인은 빈 맵)

    // 일반 로그인
    public CustomUserDetails(Member member) {
        this.member = member;
        this.attributes = Collections.emptyMap();
    }


    // OAuth2 로그인
    public CustomUserDetails(Member member, Map<String, Object> attributes) {
        this.member = member;
        this.attributes = (attributes == null) ? Collections.emptyMap() : Map.copyOf(attributes);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }


/*
    제네릭 타입 매개변수 이름이에요. 문자 하나를 임의로 붙인 것뿐이라 A, T, X 전부 가능합니다.
    무슨 뜻?
    메서드 선언 앞의 <A>는 “이 메서드는 어떤 타입 A로도 동작해”라는 뜻이고, 뒤의 A는 “그 A 타입을 반환해”라는 뜻입니다.
    즉, 호출하는 쪽에서 원하는 타입으로 받겠다는 선언이에요.
*/
/*
@SuppressWarnings("unchecked")란?

컴파일 경고 중 “unchecked(비검사) 경고”를 숨기기 위한 애너테이션이에요.

언제 뜨냐면, **제네릭 타입 정보가 런타임에 지워(타입 소거)**져서 컴파일러가 타입 안전성을 보장할 수 없을 때입니다.

예: Object → A(제네릭 형)로 캐스팅, Map<?, ?> → Map<String,Object>로 캐스팅 등

이 애너테이션은 경고만 없애줄 뿐이고, 런타임 ClassCastException 가능성은 그대로입니다.
⇒ 꼭 “여기서는 값의 실제 타입을 확신할 수 있다”는 근거가 있을 때, 가장 좁은 범위(변수 한 줄, 메서드 내부 블록)에만
쓰는 것이 좋아요.
 */
    @SuppressWarnings("unchecked")
    @Override
    public <A> A getAttribute(String name) {
        if (attributes.isEmpty() || name == null) return null;

        // 1) 1차 시도: 동일 키로 바로 조회
        Object v = attributes.get(name);
        if (v != null) return (A) v;

        // 2) 표준 키 보정(id/email/name/picture) - 구글/카카오/네이버 호환
        switch (name) {
            case "id":
                v = firstNonNull(
                        attributes.get("id"),                  // kakao
                        attributes.get("sub"),                 // google(OIDC)
                        getDeep(attributes, "response.id")     // naver
                );
                break;

            case "email":
                v = firstNonNull(
                        attributes.get("email"),                       // google(프로필에 포함될 수 있음)
                        getDeep(attributes, "kakao_account.email"),    // kakao
                        getDeep(attributes, "response.email")          // naver
                );
                break;

            case "name":
                v = firstNonNull(
                        attributes.get("name"),                        // google
                        getDeep(attributes, "properties.nickname"),    // kakao
                        getDeep(attributes, "response.name")           // naver
                );
                break;

            case "picture":
                v = firstNonNull(
                        attributes.get("picture"),                                 // google
                        getDeep(attributes, "properties.profile_image"),           // kakao (구)
                        getDeep(attributes, "kakao_account.profile.profile_image_url"), // kakao (신)
                        getDeep(attributes, "response.profile_image")              // naver
                );
                break;

            default:
                // 3) 점 표기 경로 지원: "response.email", "kakao_account.profile.profile_image_url" 등
                v = getDeep(attributes, name);
        }

        return (A) v;
    }

    private static Object firstNonNull(Object... values) {
        for (Object v : values) if (v != null) return v;
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Object getDeep(Map<String, Object> map, String path) {
        if (map == null || path == null) return null;
        String[] keys = path.split("\\.");
        Object cur = map;
        for (String k : keys) {
            if (!(cur instanceof Map)) return null;
            cur = ((Map<String, Object>) cur).get(k);
            if (cur == null) return null;
        }
        return cur;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String raw = String.valueOf(member.getRole());          // 예: USER / ROLE_USER
        String role = raw.startsWith("ROLE_") ? raw : "ROLE_" + raw;
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override public String getPassword()  { return member.getPassword(); }
    @Override public String getUsername()  { return member.getEmail(); }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }

    @Override
    public String getName() {
        // OAuth2 주 식별자 → 없으면 엔티티/이메일로 폴백
        Object id = getAttribute("id");
        if (id != null) return String.valueOf(id);
        if (member.getId() != null) return String.valueOf(member.getId());
        return member.getEmail();
    }

    // Member ID를 반환하는 메서드 추가
    public Long getMemberId() {
        return member.getId();
    }

    public Long getId() {
        return member.getId();
    }
}
