package com.project.rare_x_back.dto.request;

import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
public class ProductUpdateRequestDto {
    @Positive
    private Long brandId;

    @Positive
    private Long categoryId;

    private String productName;

    private String productDescription;

    @Positive(message = "올바른 가격을 입력해주세요")
    private Integer retailPrice;  //Integer로 둔 이유: PATCH에서 값이 안 오면 null이 들어오기 때문
}
