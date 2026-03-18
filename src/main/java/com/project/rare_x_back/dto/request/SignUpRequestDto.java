package com.project.rare_x_back.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SignUpRequestDto {

    @NotBlank(message = "이메일을 입력해주세요")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    @NotBlank(message = "비밀번호를 입력해주세요")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&?*+-_])[a-z\\d!@#$%^&?*+-_]{8,}$",
            message = "비밀번호는 영문 소문자, 숫자 ,특수문자를 포함한 8자 이상이어야 합니다"
    )   // 정규식으로 비밀번호 정책 검증(영문 소문자 1개 이상, 숫자 1개 이상, 특수문자 1개 이상, 전체 8자 이상
    private String password;

    @NotBlank(message = "비밀번호 확인을 입력해주세요")
    private String passwordConfirm;

    @Size(min = 2, max = 20, message = "이름은 2자 이상 20자 이하로 입력해주세요.")
    @Pattern(regexp = "^[가-힣a-zA-Z\\s]+$", message = "이름은 한글과 영문만 입력 가능합니다.")
    @NotBlank(message = "이름일 입력해주세요")
    private String name;
}
