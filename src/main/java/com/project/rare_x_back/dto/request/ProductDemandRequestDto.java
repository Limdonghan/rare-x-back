package com.project.rare_x_back.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductDemandRequestDto {

    @NotBlank
    @Size(min = 1, max = 30, message = "상품명은 1 ~ 30자 이내로 작성해주세요")
    private String productName;

    @NotBlank
    @Size(min = 1, max = 30, message = "브랜드명은 1 ~ 30자 이내로 작성해주세요")
    private String brandName;

    @NotNull
    private Integer retailPrice;

    @NotBlank
    @Size(min = 1, max = 300, message = "브랜드명은 1 ~ 300자 이내로 작성해주세요")
    private String description;
}
