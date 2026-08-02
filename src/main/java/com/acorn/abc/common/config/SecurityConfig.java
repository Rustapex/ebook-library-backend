package com.acorn.abc.common.config;

import com.acorn.abc.common.security.CustomAccessDeniedHandler;
import com.acorn.abc.common.security.CustomAuthenticationEntryPoint;
import com.acorn.abc.common.security.JwtAuthenticationFilter;
import com.acorn.abc.common.security.JwtTokenProvider;
import com.acorn.abc.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final MemberRepository memberRepository;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 인증은 JwtAuthenticationFilter가 전담한다. 이 bean이 없으면 Spring Boot가 매 기동마다
    // 랜덤 비밀번호를 생성해 로그에 그대로 출력한다(UserDetailsServiceAutoConfiguration). 실제로
    // 쓰이지 않는 값이라도 credential 형태로 로그에 남기지 않기 위해 빈 UserDetailsService를 등록해
    // 그 자동 구성을 비활성화한다.
    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 인증 공개 API (POST)
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/signup",
                                "/api/v1/auth/login",
                                "/api/v1/auth/find-id",
                                // Google 로그인은 아직 ABC access token이 없는 상태에서 호출된다.
                                // 정확한 3개 경로만 열고 하위 경로 wildcard는 쓰지 않는다.
                                // signup/link는 서버가 서명한 단기 transition token으로 자체 인증한다.
                                "/api/v1/auth/google",
                                "/api/v1/auth/google/signup",
                                "/api/v1/auth/google/link"
                        ).permitAll()
                        // 도서/카테고리/컬렉션/리뷰/공지 공개 API (GET)
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/auth/check-login-id",
                                "/api/v1/books",
                                "/api/v1/books/search",
                                "/api/v1/books/recommendations",
                                "/api/v1/books/{bookId}",
                                "/api/v1/books/{bookId}/libraries",
                                "/api/v1/books/{bookId}/reviews",
                                "/api/v1/categories",
                                "/api/v1/collections",
                                "/api/v1/collections/{collectionId}",
                                "/api/v1/notices",
                                "/api/v1/notices/{noticeId}"
                        ).permitAll()
                        // Swagger
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/webjars/**").permitAll()
                        // 관리자 API
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                )
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider, memberRepository),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList());
        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
