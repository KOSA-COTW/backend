package cotw.server.common.jwt;

import cotw.server.domain.member.entity.Member;
import cotw.server.domain.member.repository.MemberRepository;
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
    private final MemberRepository memberRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 헤더에서 access키에 담긴 토큰을 꺼냄
        String accessToken = request.getHeader("access");

        // Authorization 헤더도 확인 (Bearer 토큰 형식)
        if (accessToken == null) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                accessToken = authHeader.substring(7);
            }
        }

        // ✅ 토큰이 없다면 다음 필터로
        if (accessToken == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // ✅ 만료 확인 (boolean 반환값 사용)
                if (Boolean.TRUE.equals(jwtUtil.isExpired(accessToken))) {
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

        // ✅ username(email) 추출
        String username = jwtUtil.getUsername(accessToken);

        // ✅ Member 생성 (Enum에는 접두어 없는 값 사용)
        // ⭐ DB에서 Member 조회 (id, email, role 모두 포함)
        Member member = memberRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("사용자 정보 없음"));

        // ✅ UserDetails 생성
        CustomUserDetails customUserDetails = new CustomUserDetails(member);

        // ✅ 권한 목록은 "DB의 역할"을 신뢰 (토큰 role 미신뢰)
        List<SimpleGrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority(member.getRole().getAuthority()));

        // ✅ Authentication 객체 생성 및 컨텍스트 저장
        Authentication authToken =
                new UsernamePasswordAuthenticationToken(customUserDetails, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authToken);

        // ✅ 다음 필터 진행
        filterChain.doFilter(request, response);
    }
}