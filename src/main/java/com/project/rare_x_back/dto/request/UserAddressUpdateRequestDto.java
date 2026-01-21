package com.project.rare_x_back.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserAddressUpdateRequestDto {
    @NotBlank(message = "받는이를 입력해주세요.")
    private String recipientName;
    private String detailAddress;
}
