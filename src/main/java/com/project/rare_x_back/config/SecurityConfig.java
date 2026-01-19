package com.project.rare_x_back.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.rare_x_back.common.ApiResponse;
import com.project.rare_x_back.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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

                // URL별 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // 인증 없이 접근 가능한 URL
                        .requestMatchers(
                                "/api/auth/signup",
                                "/api/auth/login",
                                "/api/auth/email/send",
                                "/api/auth/email/verify",
                                "/api/auth/refresh",
                                "/api/passwordless/**"
                                "/*.html",
                                "/favicon.ico"
                        ).permitAll()

                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

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
    // ✅ CORS 허용 설정 (모든 요청 허용)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of("http://localhost:8080", "http://127.0.0.1:5500")); // 프론트엔드 주소 (필요시 "*"로 변경 가능하지만 credentials true일 땐 구체적이어야 함)
        config.addAllowedOriginPattern("*"); // 모든 Origin 허용 (테스트용)
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
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