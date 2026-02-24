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
    private String role;
    private boolean requirePasswordChange; //  true면 프론트에서 비번 변경창으로 리다이렉트
}
