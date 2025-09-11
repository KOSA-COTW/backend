package cotw.server.common.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import cotw.server.domain.member.entity.AccountStatus;
import cotw.server.domain.member.entity.Member;
import cotw.server.domain.member.repository.MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final MemberRepository memberRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 1) 화이트리스트: 로그인/리프레시
    private static final RequestMatcher SKIP_AUTH = new OrRequestMatcher(
            new AntPathRequestMatcher("/api/auth/login", "POST"),
            new AntPathRequestMatcher("/api/auth/signup", "POST"),
            new AntPathRequestMatcher("/api/recover/social", "POST"),
            new AntPathRequestMatcher("/api/reissue", "POST"),
            new AntPathRequestMatcher("/oauth2/authorization/**"),   // 소셜 로그인 시작 URL
            new AntPathRequestMatcher("/login/oauth2/code/**"),       // 콜백 URL
            new AntPathRequestMatcher("/oauth2/**"),
            new AntPathRequestMatcher("/api/public/donation-total"),
            request -> "OPTIONS".equalsIgnoreCase(request.getMethod())
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 화이트리스트는 토큰이 있어도 무조건 통과
        if (SKIP_AUTH.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = resolveAccessToken(request);

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.toLowerCase().startsWith("bearer ")) {
            token = authHeader.substring(7).trim();
        }
        if (token == null) {
            token = request.getHeader("access");
        }

        //  토큰이 없다면 다음 필터로
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 2) 만료/카테고리 검사
            if (Boolean.TRUE.equals(jwtUtil.isExpired(token))) {
                reject(response, HttpServletResponse.SC_UNAUTHORIZED, "TOKEN_EXPIRED", "access token expired");
                return;
            }
            String category = jwtUtil.getCategory(token);
            if (!"access".equals(category)) {
                reject(response, HttpServletResponse.SC_UNAUTHORIZED, "INVALID_ACCESS_TOKEN", "invalid access token category");
                return;
            }

            // 3) 클레임에서 이메일/토큰버전 추출
            String email = jwtUtil.getUsername(token);
            long tokenVersionInToken = jwtUtil.getTokenVersion(token); // <- JwtUtil에 구현 필요

            // 4) DB에서 회원 조회 후 상태/버전 검증
            Member member = memberRepository.findByEmail(email)
                    .orElse(null);
            if (member == null) {
                reject(response, HttpServletResponse.SC_UNAUTHORIZED, "USER_NOT_FOUND", "user not found");
                return;
            }
            if (member.getStatus() != AccountStatus.ACTIVE) {
                // 정지/탈퇴 등 비활성 계정
                reject(response, HttpServletResponse.SC_UNAUTHORIZED, "ACCOUNT_INACTIVE", "account inactive or deleted");
                return;
            }
            if (member.getTokenVersion() != tokenVersionInToken) {
                // 탈퇴/복구/강제 로그아웃 이후의 구토큰
                reject(response, HttpServletResponse.SC_UNAUTHORIZED, "TOKEN_STALE", "stale token (version mismatch)");
                return;
            }

            // 5) 권한은 DB의 Role을 신뢰
            List<SimpleGrantedAuthority> authorities =
                    List.of(new SimpleGrantedAuthority(member.getRole().getAuthority()));

            // 6) 인증 컨텍스트 설정
            CustomUserDetails principal = new CustomUserDetails(member);
            Authentication authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 7) 다음 필터 진행
            filterChain.doFilter(request, response);

        } catch (Exception ex) {
            log.warn("JWT processing failed: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
            reject(response, HttpServletResponse.SC_UNAUTHORIZED, "INVALID_TOKEN", "invalid or malformed token");
        }
    }

    private String resolveAccessToken(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.toLowerCase().startsWith("bearer ")) {
            return authHeader.substring(7).trim();
        }
        return null;
    }

    private void reject(HttpServletResponse response, int status, String code, String message) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), java.util.Map.of(
                "error", code,
                "message", message,
                "status", status
        ));
    }
}