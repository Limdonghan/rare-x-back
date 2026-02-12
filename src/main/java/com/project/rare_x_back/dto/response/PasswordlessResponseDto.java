package com.project.rare_x_back.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordlessResponseDto {
    private String result;
    private Object data;
    private String sessionId;
    private String pushConnectUrl;
    private String message;
    private String auth;
    private String userId;
    private String hash;

    // 성공 시 반환할 토큰 정보
    private String accessToken;
    private String refreshToken;
    private String name;
    private String role;
}
