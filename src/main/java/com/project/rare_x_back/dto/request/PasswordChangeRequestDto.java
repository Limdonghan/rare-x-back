package com.project.rare_x_back.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PasswordChangeRequestDto {

    // 임시비번 사용자는 null 가능
    private String currentPassword;

    @NotBlank(message = "새 비밀번호를 입력해주세요.")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&])[a-z\\d@$!%*?&]{8,}$",
            message = "비밀번호는 영문 소문자, 숫자, 특수문자를 포함한 8자 이상이어야 합니다"
    )
    private String newPassword;

    @NotBlank(message = "비밀번호 확인을 입력해주세요")
    private String newPasswordConfirm;

}
