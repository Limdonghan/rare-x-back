package com.project.rare_x_back.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AddressRegisterRequestDto {

    @NotBlank(message = "우편번호는 필수입니다.")
    private String postalCode;

    @NotBlank(message = "기본 주소는 필수입니다.")
    private String baseAddress;

    private String detailAddress;

    @NotBlank(message = "받는이를 입력해주세요.")
    private String recipientName;

    private boolean defaultAddress;

}
