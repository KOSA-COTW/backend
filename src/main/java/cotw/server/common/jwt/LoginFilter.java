package cotw.server.common.jwt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cotw.server.common.jwt.entity.RefreshToken;
import cotw.server.common.jwt.repository.RefreshTokenRepository;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.io.IOException;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
        String username = authentication.getName();


        //  토큰에 저장할 role은 접두사 제거해서 "ADMIN"/"USER" 형태로 표준화
        // ex) "ROLE_USER" -> "USER" 로 변환해서 토큰에 담기
        String roleFromAuth = authentication.getAuthorities().iterator().next().getAuthority();
        String roleForToken = roleFromAuth.replaceFirst("^ROLE_", "");

        //토큰 생성
        String access = jwtUtil.createToken("access", username, roleForToken, 3600000L);
        String refresh = jwtUtil.createToken("refresh", username, roleForToken, 86400000L);

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
        response.setHeader("Authorization", "Bearer " + access);
        response.addHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Authorization");

        response.setStatus(HttpStatus.OK.value());
    }

    @Override
    protected void unsuccessfulAuthentication(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException failed
    ) {
        log.warn("Login failed: {}", failed.getMessage());
        log.warn("Login failed: {}", failed.getClass());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        try {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Invalid credentials");
            objectMapper.writeValue(response.getWriter(), errorResponse);
        } catch (IOException e) {
            log.error("Error writing error response", e);
        }
    }

    private void addRefreshToken(String email, String refreshToken, Long expiredMs) {
        RefreshToken checkToken = refreshTokenRepository.findByRefreshToken(refreshToken);
//        if (checkToken == null) {
            Date date = new Date(System.currentTimeMillis() + expiredMs);

            RefreshToken refreshTokenEntity = new RefreshToken();
            refreshTokenEntity.setEmail(email);
            refreshTokenEntity.setRefreshToken(refreshToken);
            refreshTokenEntity.setExpiryDate(date.toString());

            refreshTokenRepository.save(refreshTokenEntity);
//        }
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
