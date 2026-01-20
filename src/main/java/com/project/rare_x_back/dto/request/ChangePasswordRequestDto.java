package com.project.rare_x_back.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequestDto {

     //현재 비밀번호
     //임시 비밀번호 사용자는 null 가능
    private String currentPassword;
     //새 비밀번호
    @NotBlank(message = "새 비밀번호를 입력해주세요.")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,20}$",
            message = "비밀번호는 8-20자, 영문, 숫자, 특수문자를 포함해야 합니다"
    )
    private String newPassword;

   // 새 비밀번호 확인
    @NotBlank(message = "새로 등록하실 비밀번호를 한 번 더 입력해주세요.")
    private String newPasswordConfirm;
}

