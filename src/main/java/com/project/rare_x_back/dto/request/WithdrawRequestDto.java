package com.project.rare_x_back.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithdrawRequestDto {
    private String password;  // 패스워드리스 사용자는 null 허용
}