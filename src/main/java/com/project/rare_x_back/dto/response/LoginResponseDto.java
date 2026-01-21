package com.project.rare_x_back.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDto {
    private String accessToken;
    private String refreshToken;
    private String name;
    private boolean isPasswordChangeRequired;
    private String role;

    /// [추가] 패스워드리스 일회용 토큰
    private Object passwordlessToken;
}
