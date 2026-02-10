package com.project.rare_x_back.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSearchResponseDto {
    private Long productId;
    private String productName;
    private String brandName;
    private String categoryName;
    private String productDescription;
    private int retailPrice;
    private List<String> imageUrls;    /// [추가] 이미지 URL
    private int wishCount;      /// [추가] 관심 수

}
