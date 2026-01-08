package com.project.rare_x_back.security;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenValidity;
    private final long refreshTokenValidity;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity}") long accessTokenValidity,
            @Value("${jwt.refresh-token-validity}") long refreshTokenValidity) {

        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidity = accessTokenValidity;
        this.refreshTokenValidity = refreshTokenValidity;
    }

    //  Access Token 생성
    public String createAccessToken(Long userId) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + accessTokenValidity);

        return Jwts.builder()
                .subject(String.valueOf(userId))    // 사용자 ID
                .issuedAt(now)
                .expiration(validity)   // 만료 시간 (1시간)
                .signWith(secretKey)    // 시크릿 키 발급
                .compact();     // 문자열로 만들기
    }

    //  Refresh Token 생성
    public String createRefreshToken(Long userId) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + refreshTokenValidity);

        return Jwts.builder()
                .subject(String.valueOf(userId))    // 사용자 ID만
                .issuedAt(now)
                .expiration(validity)   // 만료 시간 (7일)
                .signWith(secretKey)
                .compact();
    }

    //  토큰에서 userId 추출
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return Long.parseLong(claims.getSubject());
    }

    // 토큰 유효성 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)  // 시크릿키 검증
                    .build()
                    .parseSignedClaims(token); // 토큰 파싱 시도 (검증 + 내용 읽기)
            return true;    // 유효
        } catch (JwtException | IllegalArgumentException e) {
            return false;   // 위조
        }
    }

    // 토큰의 남은 만료 시간 조회 (밀리초)
    public long getExpiration(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Date expiration = claims.getExpiration();
            long now = System.currentTimeMillis();

            return expiration.getTime() - now;
        } catch (JwtException | IllegalArgumentException e) {
            return 0;
        }
    }
}
