package com.project.rare_x_back.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProductUpdateRequestDto {
    private Long brandId;
    private Long categoryId;
    private String productName;
    private String productDescription;
    private Integer retailPrice;  //Integer로 둔 이유: PATCH에서 값이 안 오면 null이 들어오기 때문
}
