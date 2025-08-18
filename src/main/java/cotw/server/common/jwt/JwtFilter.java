package cotw.server.common.jwt;

import cotw.server.domain.member.entity.Member;
import cotw.server.domain.member.entity.Role;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // ✅ Authorization 헤더 또는 access 헤더에서 토큰 추출
        String accessToken = null;
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        } else {
            accessToken = request.getHeader("access");
        }

        // ✅ 토큰이 없다면 다음 필터로
        if (accessToken == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // ✅ 만료 확인
        try {
            jwtUtil.isExpired(accessToken);
        } catch (ExpiredJwtException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            try (PrintWriter writer = response.getWriter()) {
                writer.print("access token expired");
            }
            return;
        }

        // ✅ category 확인
        String category = jwtUtil.getCategory(accessToken);
        if (!"access".equals(category)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            try (PrintWriter writer = response.getWriter()) {
                writer.print("invalid access token");
            }
            return;
        }

        // ✅ username(email), role 추출
        String username = jwtUtil.getUsername(accessToken);
        String roleFromToken = jwtUtil.getRole(accessToken); // ex) "USER" or "ADMIN"

        // Spring Security 권한 규칙에 맞춰 접두어 추가
        String roleWithPrefix = roleFromToken.startsWith("ROLE_")
                ? roleFromToken
                : "ROLE_" + roleFromToken;

        // ✅ Member 생성 (Enum에는 접두어 없는 값 사용)
        Member member = new Member();
        member.setEmail(username);
        member.setRole(Role.valueOf(roleFromToken.replace("ROLE_", "")));

        // ✅ UserDetails 생성
        CustomUserDetails customUserDetails = new CustomUserDetails(member);

        // ✅ 권한 목록 생성
        List<SimpleGrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority(roleWithPrefix));

        // ✅ Authentication 객체 생성 및 컨텍스트 저장
        Authentication authToken =
                new UsernamePasswordAuthenticationToken(customUserDetails, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authToken);

        // ✅ 다음 필터 진행
        filterChain.doFilter(request, response);
    }
}
