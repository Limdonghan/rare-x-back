package com.project.rare_x_back.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BillingKeyRequestDto {

    @NotBlank
    private String customerKey;  /// 유저 고유 식별키

    @NotBlank
    private String authKey;     /// 프론트에서 받아온 임시 인증키
}
