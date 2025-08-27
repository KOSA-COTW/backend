package cotw.server.common.security;

import cotw.server.common.auth.CustomOAuth2UserService;
import cotw.server.common.auth.OAuth2LoginSuccessHandler;
import cotw.server.common.jwt.*;
import cotw.server.common.jwt.repository.RefreshTokenRepository;
import cotw.server.domain.member.repository.MemberRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oauth2LoginSuccessHandler;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;
    private final MemberStatusPostChecker memberStatusPostChecker;

    @Bean
    public AuthenticationManager authenticationManager() throws Exception {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);

        // 계정수집 방지: 비번 검증 "전" 상태 체크는 끈다(또는 no-op로 대체)
        provider.setPreAuthenticationChecks(user -> {});

        // 비번 검증 "후" 상태로 분기
        provider.setPostAuthenticationChecks(memberStatusPostChecker);

        return new ProviderManager(provider);
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
            config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
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
                .requestMatchers(HttpMethod.GET, "/info").permitAll()
                // 소프트 삭제 관련 요청
                .requestMatchers(HttpMethod.POST, "/deactivate", "/recover").permitAll()

                .requestMatchers(HttpMethod.PATCH, "/editpass", "/changeimage").permitAll()

                // 공개 조회
                .requestMatchers(HttpMethod.GET, "/api/posts").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/posts/**").permitAll()

                // 생성 권한
                .requestMatchers(HttpMethod.POST, "/api/posts").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/posts/admin").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/posts/admin/public").hasRole("ADMIN")

                .requestMatchers(HttpMethod.GET, "/api/notices/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/notices/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/notices/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/notices/**").hasRole("ADMIN")

                // 관리자 전용
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/posts/visibility").hasRole("ADMIN")

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


        //세션 설정
        http
                .sessionManagement((session) -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http
                // 인증 실패 시 401을 주고, 리다이렉트하지 않게
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                        .accessDeniedHandler((req, res, ex) -> res.sendError(HttpServletResponse.SC_FORBIDDEN))
                );

        return http.build();
    }
}
