package cotw.server.common.security;

import cotw.server.common.auth.CustomOAuth2UserService;
import cotw.server.common.auth.OAuth2LoginSuccessHandler;
import cotw.server.common.jwt.CustomLogoutFilter;
import cotw.server.common.jwt.JwtFilter;
import cotw.server.common.jwt.JwtUtil;
import cotw.server.common.jwt.LoginFilter;
import cotw.server.common.jwt.repository.RefreshTokenRepository;
import cotw.server.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthenticationConfiguration authenticationConfiguration;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oauth2LoginSuccessHandler;
    private final MemberRepository memberRepository;

    @Bean
    public AuthenticationManager authenticationManager() throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** 커스텀 로그인 필터 (ID/PW → JWT 발급) */
    @Bean
    public LoginFilter loginFilter() throws Exception {
        LoginFilter loginFilter = new LoginFilter(
                authenticationManager(),
                jwtUtil,
                refreshTokenRepository
        );
        loginFilter.setFilterProcessesUrl("/auth/login"); // 로그인 엔드포인트
        return loginFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // CORS
        http.cors(cors -> cors.configurationSource(request -> {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowedOrigins(List.of("http://localhost:5173"));
            config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
            config.setAllowedHeaders(List.of("*"));
            config.setExposedHeaders(List.of("Authorization"));
            config.setAllowCredentials(true); // refresh 토큰 쿠키 사용 시 필수
            config.setMaxAge(3600L);
            return config;
        }));

        // 기본 인증 비활성화
        http.csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);

        // 인가 정책
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/oauth2/**", "/login/oauth2/code/**").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/", "/auth/login", "/auth/signup", "/reissue").permitAll()
                .requestMatchers("/api/payments/success", "/api/payments/confirm").permitAll()

                .requestMatchers(HttpMethod.GET, "/api/posts").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/posts/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/posts").authenticated()

                // 관리자 전용
                .requestMatchers("/api/admin/**").hasRole("ADMIN")


                .anyRequest().authenticated()
        );

        // OAuth2 로그인
        http.oauth2Login(oauth -> oauth
                .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                .successHandler(oauth2LoginSuccessHandler)
        );

        // 필터 체인 (순서 중요)
        http
                // 1) JWT 인증 필터: 모든 요청 전에 토큰 해석/인증
                .addFilterBefore(new JwtFilter(jwtUtil, memberRepository), UsernamePasswordAuthenticationFilter.class)
                // 2) 커스텀 로그인 필터: /auth/login 처리하여 JWT 발급
                .addFilterAt(loginFilter(), UsernamePasswordAuthenticationFilter.class)
                // 3) 커스텀 로그아웃 필터: 로그아웃/리프레시토큰 제거 등
                .addFilterBefore(new CustomLogoutFilter(jwtUtil, refreshTokenRepository), LogoutFilter.class);

        // 세션 비활성화 (JWT 방식)
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }
}
