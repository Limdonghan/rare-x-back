package com.project.rare_x_back.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDto {
    private Long productId;
    private String productName;
    private String brandName;
    private String categoryName;
    private String imageUrl;
    private String productDescription;
    private int price;
    private List<String> imageUrls;
    private int retailPrice;
    private int wishCount;          /// [추가] 관심 수
}
