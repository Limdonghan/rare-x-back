package com.project.rare_x_back.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.rare_x_back.common.ApiResponse;
import com.project.rare_x_back.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity      // (보안 스위치 1) "우리 사이트 출입 통제 시스템(Spring Security)을 가동하겠다!"는 뜻
@EnableMethodSecurity   // (보안 스위치 2) "메서드마다 개별 잠금장치를 달 수 있게 하겠다!"는 뜻 (예: 특정 기능에 @PreAuthorize 붙이기 가능)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;  // JSON 변환용

    @Value("${CSP_MODE:dev}")
    private String cspMode;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화 (JWT 사용)
                .csrf(AbstractHttpConfigurer::disable)

                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // ✅ CORS 설정 추가

                // 세션 사용 안 함 (JWT 사용)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                //  CSP 및 보안 헤더 추가
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(
                                        isProd() ? prodCsp() : devCsp()
                                )
                        )
                        .frameOptions(frame -> frame.sameOrigin())
                )

                // URL별 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // 인증 없이 접근 가능한 URL
                        .requestMatchers(
                                "/api/auth/signup",
                                "/api/auth/login",
                                "/api/auth/email/send",
                                "/api/auth/email/verify",
                                "/api/auth/refresh",
                                "/api/passwordless/**",
                                "/*.html",
                                "/favicon.ico",
                                "/api/product/**",
                                "/api/serving/login-trigger",
                                "/api/serving/result",
                                "/api/serving/cancel",
                                "/actuator/health",
                                "/api/auth/users/resetpw",
                                "/api/product/ranking",
                                "/api/serving/status"
                        ).permitAll()
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()
                        // wish 경로는 인증 필수 (순서 permitAll보다 먼저)
                        .requestMatchers("/api/product/*/wish").authenticated()
                        // 나머지 상품 조회는 비로그인 허용
                        .requestMatchers("/api/product/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/inspections/**").hasRole("ADMIN")

                        // 그 외 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )

                // 예외 처리 (ApiResponse 형식 사용)
                .exceptionHandling(exception -> exception
                        // 인증 실패 시 (401)
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");

                            // ApiResponse 형식으로 반환!
                            ApiResponse<Void> errorResponse = ApiResponse.error(
                                    "UNAUTHORIZED",
                                    "인증이 필요합니다. 로그인 후 다시 시도해주세요."
                            );

                            response.getWriter().write(
                                    objectMapper.writeValueAsString(errorResponse)
                            );
                        })

                        // 권한 부족 시 (403)
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json;charset=UTF-8");

                            // ApiResponse 형식으로 반환
                            ApiResponse<Void> errorResponse = ApiResponse.error(
                                    "FORBIDDEN",
                                    "접근 권한이 없습니다."
                            );

                            response.getWriter().write(
                                    objectMapper.writeValueAsString(errorResponse)
                            );
                        })
                )

                // JWT 필터 추가
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    // 운영 CSP
    private String prodCsp() {
        return
                "default-src 'self'; " +

                        "script-src 'self' " +
                        "https://js.tosspayments.com " +
                        "https://www.juso.go.kr " +
                        "https://toss.im; " +

                        "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; " +

                        "img-src 'self' data: https://4tential-rare-x.s3.amazonaws.com; " +

                        "font-src 'self' data: https://cdn.jsdelivr.net; " +

                        "connect-src 'self' " +
                        "https://api.tosspayments.com " +
                        "https://www.juso.go.kr " +
                        "https://toss.im; " +

                        "frame-src 'self' " +
                        "https://www.juso.go.kr " +
                        "https://toss.im; " +

                        "frame-ancestors 'self';";
    }

    // 개발 CSP
    private String devCsp() {
        return
                "default-src 'self'; " +

                        "script-src 'self' 'unsafe-inline' 'unsafe-eval' " +
                        "https://js.tosspayments.com " +
                        "https://www.juso.go.kr " +
                        "https://toss.im; " +

                        "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; " +

                        "img-src 'self' data: https://4tential-rare-x.s3.amazonaws.com; " +

                        "font-src 'self' data: https://cdn.jsdelivr.net; " +

                        "connect-src 'self' " +
                        "http://localhost:8080 " +
                        "http://localhost:5173 " +
                        "ws://localhost:5173 " +
                        "https://api.tosspayments.com " +
                        "https://www.juso.go.kr " +
                        "https://toss.im; " +

                        "frame-src 'self' " +
                        "https://www.juso.go.kr " +
                        "https://toss.im; " +

                        "frame-ancestors 'self';";
    }

    // prod 여부 판별 메서드
    private boolean isProd() {
        return "prod".equalsIgnoreCase(cspMode);
    }


    // CORS 허용 설정 (모든 요청 허용)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);

        // 환경 분기
        if (isProd()) {

            // 운영
            config.setAllowedOrigins(List.of(
                    "https://rarex.club",
                    "https://www.rarex.club"
            ));

        } else {

            // 개발
            config.setAllowedOrigins(List.of(
                    "http://localhost:5173",
                    "http://localhost:3000"
            ));
        }

        config.setAllowedMethods(List.of(
                "GET","POST","PUT","DELETE","PATCH","OPTIONS"
        ));

        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }



    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 멀티파트 해석기 -> 전송된 파일 데이터를 MultipartFile 객체로 변환하여 컨트롤러에 넘겨줌
    @Bean
    public MultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }
}