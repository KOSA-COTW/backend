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
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
public class LoginFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Authentication attemptAuthentication(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws AuthenticationException {      // json 타입을 파싱하여 사용.
        if (request.getContentType().equals("application/json")) {
            try {
                Map<String, String> credentials = new ObjectMapper().readValue(request.getInputStream(), new TypeReference<>() {});
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
        //유저 정보
        String username = authentication.getName();

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
        GrantedAuthority auth = iterator.next();
        String role = auth.getAuthority();

        // ex) "ROLE_USER" -> "USER" 로 변환해서 토큰에 담기
        String roleFromAuth = authentication.getAuthorities().iterator().next().getAuthority();
        String roleForToken = roleFromAuth.replaceFirst("^ROLE_", "");

        //토큰 생성
        String access = jwtUtil.createToken("access", username, roleForToken, 3600000L);
        String refresh = jwtUtil.createToken("refresh", username, roleForToken, 86400000L);

        // refresh token save
        addRefreshToken(username, refresh, 1000*60*60*24L);

        //응답 설정
        response.setHeader("access", access);
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
//        cookie.setHttpOnly(true);
//        cookie.setPath("/"); // 전역 경로 설정
        cookie.setSecure(true); // HTTPS에서만 전송
        return cookie;
    }
}
