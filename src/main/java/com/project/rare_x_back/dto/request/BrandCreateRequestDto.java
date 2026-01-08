package com.project.rare_x_back.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BrandCreateRequestDto {
    @NotBlank(message = "브랜드명을 입력해 주세요.")
    private String brandName;
}
