package cotw.server.common.auth;

import cotw.server.common.jwt.JwtUtil;
import cotw.server.common.jwt.entity.RefreshToken;
import cotw.server.common.jwt.repository.RefreshTokenRepository;
import cotw.server.domain.member.entity.Member;
import cotw.server.domain.member.entity.ProviderType;
import cotw.server.domain.member.repository.MemberRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Duration;
import java.util.Date;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    private final String frontRedirectBase = "http://localhost:5173/oauth2/success";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        String registrationId = oauthToken.getAuthorizedClientRegistrationId(); // "google"/"kakao"/"naver"
        OAuth2User oAuth2User = (OAuth2User) oauthToken.getPrincipal();
        Map<String, Object> attrs = oAuth2User.getAttributes();

        // provider / providerId 식별
        ProviderType provider = toProviderType(registrationId);
        String providerId = resolveProviderId(attrs);

        // 멤버 조회 (CustomOAuth2UserService에서 upsert 했으므로 반드시 존재)
        Member member = memberRepository
                .findByProviderAndProviderId(provider, providerId)
                .orElseThrow(() -> new IllegalStateException("OAuth2 user upsert missing: " + provider + ":" + providerId));


        String role = member.getRole().name(); // 예: USER → ROLE_USER
        long v = member.getTokenVersion();
        long memberId = member.getId();

        // 3) JWT 생성
        String access  = jwtUtil.createToken("access",  member.getEmail(), role, memberId, v, 10 * 60 * 1000L);      // 10분
        String refresh = jwtUtil.createToken("refresh", member.getEmail(), role, memberId, v, 24 * 60 * 60 * 1000L); // 24시간

        // 중복된 토큰이 존재 할 시 기존 것을 삭제 후 저장
        refreshTokenRepository.deleteByEmail(member.getEmail());


        // refresh 토큰 저장 (DB)
        saveRefreshToken(member.getEmail(), refresh, 24 * 60 * 60 * 1000L);

        // 응답 구성
        response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access);
        response.addHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Authorization");

        // Refresh 토큰은 HttpOnly 쿠키로
        ResponseCookie refreshCookie = ResponseCookie.from("refresh", refresh)
                .httpOnly(true)
                .secure(false)               // 로컬 http 테스트면 false, https 환경에서 true
                .sameSite("None")           // 프론트/백이 다른 오리진이면 필수
                .path("/")
                .maxAge(Duration.ofDays(1))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());


        // 6) 프론트로 리다이렉트
        // - 헤더는 리다이렉트 후 JS에서 못 읽으므로, 짧게는 해시(#)에 access를 실어 전달 가능
        // - 더 안전한 방식은 코드(1회용 key)를 발급하고 프론트가 /api/auth/exchange 로 교환하는 방식

        String redirectUrl = UriComponentsBuilder
                .fromUriString(frontRedirectBase)
                .fragment("access=" + access)
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }

    private void saveRefreshToken(String email, String refresh, long expiresMs) {
        Date expiry = new Date(System.currentTimeMillis() + expiresMs);
        RefreshToken entity = new RefreshToken();
        entity.setEmail(email);
        entity.setRefreshToken(refresh);
        entity.setExpiryDate(expiry.toString());
        refreshTokenRepository.save(entity);
    }

    private ProviderType toProviderType(String registrationId) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> ProviderType.GOOGLE;
            case "kakao"  -> ProviderType.KAKAO;
            case "naver"  -> ProviderType.NAVER;
            default -> throw new IllegalArgumentException("Unsupported provider: " + registrationId);
        };
    }

    // 구글/카카오/네이버 대응: attributes에서 표준 id를 찾아냄
    private String resolveProviderId(Map<String, Object> attrs) {
        Object id = firstNonNull(
                attrs.get("id"),
                attrs.get("sub"),
                getDeep(attrs, "response.id")
        );
        if (id == null) throw new IllegalStateException("OAuth2 attributes missing id/sub/response.id");
        return String.valueOf(id);
    }

    private static Object firstNonNull(Object... vals) {
        for (Object v : vals) if (v != null) return v;
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
}
