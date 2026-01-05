package com.project.rare_x_back.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProductCreateRequestDto {
    private Long brandId;
    private Long categoryId;
    private String productName;
    private String productDescription;
    private int retailPrice;
}
