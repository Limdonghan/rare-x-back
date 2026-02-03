package com.project.rare_x_back.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserWalletAccountResponseDto {
    private Long balance;
    private String email;
    private String name;
    private String formattedBalance; // 천 단위 콤마

}
