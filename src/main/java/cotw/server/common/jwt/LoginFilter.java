package cotw.server.common.jwt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cotw.server.common.jwt.entity.RefreshToken;
import cotw.server.common.jwt.repository.RefreshTokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
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
    ) throws AuthenticationException {

        String contentType = request.getContentType();
        // application/json; charset=UTF-8 등도 허용
        if (contentType != null && contentType.toLowerCase().startsWith("application/json")) {
            try {
                Map<String, String> credentials =
                        objectMapper.readValue(request.getInputStream(), new TypeReference<>() {});
                String email = credentials.get("email");
                String password = credentials.get("password");

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


        // 토큰에 저장할 role은 접두사 제거해서 "ADMIN"/"USER" 형태로 표준화
        String roleFromAuth = authentication.getAuthorities().iterator().next().getAuthority();
        String roleForToken = roleFromAuth.replaceFirst("^ROLE_", "");

        // 토큰 생성 (ms)
        String access  = jwtUtil.createToken("access",  username, roleForToken, null, 3_600_000L);   // 1시간
        String refresh = jwtUtil.createToken("refresh", username, roleForToken, null, 86_400_000L);  // 24시간

        // refresh token 저장(DB)
        addRefreshToken(username, refresh, 86_400_000L);

        // 응답 설정
        response.setHeader("Authorization", "Bearer " + access);
        response.addHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Authorization");

        // refresh 토큰은 쿠키로
        response.addCookie(createCookie("refresh", refresh));

        response.setStatus(HttpStatus.OK.value());
    }

    @Override
    protected void unsuccessfulAuthentication(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException failed
    ) {
        log.warn("Login failed: {}", failed.getMessage());
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
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setSecure(false);
        return cookie;
    }
}
