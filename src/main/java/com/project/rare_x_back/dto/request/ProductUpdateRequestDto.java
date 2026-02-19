package com.project.rare_x_back.dto.request;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
public class ProductUpdateRequestDto {
    @Positive
    private Long brandId;

    @Positive
    private Long categoryId;

    @Size(max = 100, message = "상품명은 100자 이내여야 합니다.")
    private String productName;

    private String productDescription;

    @Positive(message = "올바른 가격을 입력해주세요")
    private Integer retailPrice;  //Integer로 둔 이유: PATCH에서 값이 안 오면 null이 들어오기 때문
}
