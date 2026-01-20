package com.project.rare_x_back.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordlessRequestDto {

    @Email(message = "올바른 이메일 형식이 아닙니다")
    @NotBlank(message = "사용자 ID는 필수입니다.")
    private String userId;  //필수 값, email

    private String token;   //토큰
    private String sessionId;   //세션아이디
    private String qrReg;   //QR 등록여부
}
