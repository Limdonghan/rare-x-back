package com.project.rare_x_back.security;

import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {         // OncePerRequestFilter : 모든 HTTP 요청마다 한 번씩 실행(요청 → 필터 → Controller 순서)

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            // 1. Authorization 헤더에서 토큰 추출
            String token = getTokenFromRequest(request);

            // 2. 토큰이 있고 유효한 경우
            if (token != null && jwtTokenProvider.validateToken(token)) {

                // 블랙리스트 확인
                if (tokenBlacklistService.isBlacklisted(token)) {
                    log.debug("블랙리스트 토큰: {}", token.substring(0, 20));
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }

                // 3. 토큰에서 userId 추출,  role 추출
                Long userId = jwtTokenProvider.getUserIdFromToken(token);
                String role = jwtTokenProvider.getRoleFromToken(token);

                // 4. Spring Security 인증 객체 생성 (실제 role 사용)
                List<SimpleGrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + role)
                );

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,      // principal (사용자 식별자)
                                null,        // credentials (비밀번호, 불필요)
                                authorities  // authorities (기본 USER 권한)
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // 5. SecurityContext에 인증 정보 저장
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("JWT 인증 성공: userId={}, role={}", userId, role);
            }

        } catch (CustomException e) {
            log.error("JWT 인증 실패: {}", e.getMessage());
            // 인증 실패 시 SecurityContext를 비워둠
            SecurityContextHolder.clearContext();
        }

        // 다음 필터로 진행
        filterChain.doFilter(request, response);
    }


    //  Authorization 헤더에서 Bearer 토큰 추출
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);  // "Bearer " 제거
        }

        return null;
    }
}