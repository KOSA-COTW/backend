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

        // Authorization: Bearer <token> 우선, 없으면 기존 "access" 헤더
        String accessToken = null;
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        } else {
            accessToken = request.getHeader("access");
        }

        // 토큰이 없다면 다음 필터
        if (accessToken == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 만료 확인
        try {
            jwtUtil.isExpired(accessToken);
        } catch (ExpiredJwtException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            try (PrintWriter writer = response.getWriter()) {
                writer.print("access token expired");
            }
            return;
        }

        // category 확인
        String category = jwtUtil.getCategory(accessToken);
        if (!"access".equals(category)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            try (PrintWriter writer = response.getWriter()) {
                writer.print("invalid access token");
            }
            return;
        }

        // username(email), role 추출
        String username = jwtUtil.getUsername(accessToken);
        String role = jwtUtil.getRole(accessToken);
        String roleFromToken = jwtUtil.getRole(accessToken); // ex) "USER"
        String roleWithPrefix = roleFromToken.startsWith("ROLE_") ? roleFromToken : "ROLE_" + roleFromToken; // ex) "ROLE_USER"

        // enum 매칭은 접두어 없는 값 사용
        // "ROLE_ADMIN" 형태면 접두사 제거하여 Enum 매핑
        if (role != null && role.startsWith("ROLE_")) {
            role = role.substring(5); // "ADMIN"/"USER"/"ORGANIZATION"
        }

        Member member = new Member();
        member.setEmail(username);
        member.setRole(Role.valueOf(roleFromToken));
        member.setRole(Role.valueOf(role));

        // 필요 시: 토큰에 userId 클레임이 있으면 세팅
        // Long userId = jwtUtil.getUserId(accessToken);
        // if (userId != null) member.setId(userId);

        CustomUserDetails customUserDetails = new CustomUserDetails(member);

        Authentication authToken =
                new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());
        // 권한 목록 생성 (Spring Security 권한 규칙에 맞춰 접두어 포함)
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(roleWithPrefix));

        // Authentication 객체 생성 및 컨텍스트에 저장
        Authentication authToken = new UsernamePasswordAuthenticationToken(customUserDetails, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }
}
