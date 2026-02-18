package com.project.rare_x_back.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProductCreateRequestDto {
    @NotNull(message = "브랜드는 필수로 선택해야 합니다.")
    private Long brandId;

    @NotNull(message = "카테고리는 필수로 선택해야 합니다.")
    private Long categoryId;

    @NotBlank(message = "상품명은 필수입니다.")
    @Size(max = 20, message = "상품명은 20자 이내여야 합니다.")
    private String productName;

    @NotBlank(message = "상품 설명을 입력해주세요.")
    @Size(max = 100, message = "상품 설명은 100자 이내로 작성해주세요.")
    private String productDescription;

    @NotNull(message = "상품의 발매가를 입력해주세요.")
    @Positive(message = "올바른 가격을 입력해주세요")
    private Integer retailPrice;
}
