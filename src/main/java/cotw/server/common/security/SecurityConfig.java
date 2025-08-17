package cotw.server.common.security;

import cotw.server.common.jwt.CustomLogoutFilter;
import cotw.server.common.jwt.JwtFilter;
import cotw.server.common.jwt.JwtUtil;
import cotw.server.common.jwt.LoginFilter;
import cotw.server.common.jwt.repository.RefreshTokenRepository;
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
public class SecurityConfig {

    private final AuthenticationConfiguration authenticationConfiguration;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    public SecurityConfig(AuthenticationConfiguration authenticationConfiguration,
                          JwtUtil jwtUtil,
                          RefreshTokenRepository refreshTokenRepository) {
        this.authenticationConfiguration = authenticationConfiguration;
        this.jwtUtil = jwtUtil;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
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
            config.setMaxAge(3600L);
            return config;
        }));



        //csrf disable
        http
                .csrf(AbstractHttpConfigurer::disable);

        //From 로그인 방식 disable
        http
                .formLogin(AbstractHttpConfigurer::disable);

        //http basic 인증 방식 disable
        http
                .httpBasic(AbstractHttpConfigurer::disable);

        // 권한 정책
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/", "/login", "/member/signup", "/reissue").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN") // ✅ 관리자 보호
                .anyRequest().authenticated()
        );

        http
                .addFilterBefore(new JwtFilter(jwtUtil), LoginFilter.class)
                .addFilterAt(new LoginFilter(authenticationManager(authenticationConfiguration), jwtUtil, refreshTokenRepository),
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new CustomLogoutFilter(jwtUtil, refreshTokenRepository), LogoutFilter.class);


        //세션 설정
        http
                .sessionManagement((session) -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }
}
