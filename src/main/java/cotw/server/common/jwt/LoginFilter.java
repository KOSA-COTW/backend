package cotw.server.common.jwt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cotw.server.common.jwt.entity.RefreshToken;
import cotw.server.common.jwt.repository.RefreshTokenRepository;
import cotw.server.domain.member.entity.AccountStatus;
import cotw.server.domain.member.entity.Member;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

@Slf4j
public class LoginFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LoginFilter(AuthenticationManager authenticationManager, JwtUtil jwtUtil, RefreshTokenRepository refreshTokenRepository) {
        super();
        setAuthenticationManager(authenticationManager);
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    public Authentication attemptAuthentication(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws AuthenticationException {      // json 타입을 파싱하여 사용.

        String contentType = request.getContentType();
        // application/json; charset=UTF-8 등도 허용
        if (contentType != null && contentType.toLowerCase().startsWith("application/json")) {
            try {
                Map<String, String> credentials = new ObjectMapper().readValue(request.getInputStream(), new TypeReference<>() {});
                String email = credentials.get("email");
                String password = credentials.get("password");

                log.debug("Login JSON branch, email={}", email);

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(email, password);

                return authenticationManager.authenticate(authToken);

            } catch (IOException e) {
                throw new AuthenticationServiceException("Request parsing failed", e);
            }
        }
        return super.attemptAuthentication(request, response);
    }

    @Override
    protected void successfulAuthentication(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain,
            Authentication authentication
    ) {
        // 유저 정보

        CustomUserDetails cud = (CustomUserDetails) authentication.getPrincipal();
        Member m = cud.getMember();

        if (m.getStatus() != AccountStatus.ACTIVE) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return; // 안전장치
        }

        String username = m.getEmail();
        long v = m.getTokenVersion();


        //  토큰에 저장할 role은 접두사 제거해서 "ADMIN"/"USER" 형태로 표준화
        // ex) "ROLE_USER" -> "USER" 로 변환해서 토큰에 담기
        String roleFromAuth = authentication.getAuthorities().iterator().next().getAuthority();
        String roleForToken = roleFromAuth.replaceFirst("^ROLE_", "");

        //토큰 생성
        String access = jwtUtil.createToken("access", username, roleForToken, m.getId(), v,3600000L);
        String refresh = jwtUtil.createToken("refresh", username, roleForToken, m.getId(), v, 86400000L);

        // refresh token save
        addRefreshToken(username, refresh, 1000*60*60*24L);

        //응답 설정
        response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access);

        // 프론트가 Authorization 헤더를 읽을 수 있게 노출(전역 CORS에서 하는 게 더 좋음)
        response.addHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Authorization");

        // refresh는 HttpOnly 쿠키로(크로스도메인 테스트면 SameSite=None; Secure 필수)
        ResponseCookie refreshCookie = ResponseCookie.from("refresh", refresh)
                .httpOnly(true)
                .secure(false)      // 개발이 http라면 false 또는 프록시/https로 테스트
                .path("/")
                .sameSite("None")  // 크로스 도메인일 때 필수
                .maxAge(Duration.ofDays(1))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        // 응답 설정
        // 표준 Authorization 헤더 사용 (프론트가 쉽게 읽을 수 있도록)
        response.setHeader("Authorization", "Bearer " + access);

        // (선택) 레거시 호환: 기존 "access" 헤더도 함께 넣어둠. 점진적 제거 가능.
        response.setHeader("access", access);

        response.setStatus(HttpStatus.OK.value());
    }

    @Override
    protected void unsuccessfulAuthentication(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException failed
    ) {
        String code = "AUTH_FAILED";
        int status = HttpServletResponse.SC_UNAUTHORIZED;

        if (failed instanceof LockedException) {
            code = "ACCOUNT_SUSPENDED"; status = 423; // Locked
        } else if (failed instanceof DisabledException) {
            code = "ACCOUNT_DELETED"; status = 403;
        } else if (failed instanceof AccountExpiredException) {
            code = "ACCOUNT_PENDING"; status = 409;
        } else if (failed instanceof CredentialsExpiredException) {
            code = "PASSWORD_EXPIRED"; status = 401;
        } else if (failed instanceof BadCredentialsException) {
            code = "BAD_CREDENTIALS"; status = 401;
        }


        log.warn("Login failed: {}", failed.getMessage());
        log.warn("Login failed: {}", failed.getClass());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(status);

        try {
            objectMapper.writeValue(response.getWriter(), Map.of(
                    "error", code,
                    "message", code   // 필요 시 i18n으로 치환
            ));
        } catch (IOException e) {
            log.error("Error writing error response", e);
        }
    }

    private void addRefreshToken(String email, String refreshToken, Long expiredMs) {
            Date date = new Date(System.currentTimeMillis() + expiredMs);

            RefreshToken refreshTokenEntity = new RefreshToken();
            refreshTokenEntity.setEmail(email);
            refreshTokenEntity.setRefreshToken(refreshToken);
            refreshTokenEntity.setExpiryDate(date.toString());

            refreshTokenRepository.save(refreshTokenEntity);
    }

    private Cookie createCookie(String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(24 * 60 * 60); // 24시간
        cookie.setHttpOnly(true);       // JS에서 접근 불가
        cookie.setPath("/");            // 전역
        cookie.setSecure(false);        // 로컬 http 개발에선 false, 배포(HTTPS)에서는 true로!
        return cookie;
    }
}
