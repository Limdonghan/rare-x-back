package com.project.rare_x_back.dto.response;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RefreshTokenResponseDto {
    private String accessToken;
    private String refreshToken;
}
