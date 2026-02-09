package com.project.rare_x_back.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StorageProductResponseDto {
    private Long productId;
    private String brandName;
    private String productName;
    private int price;      // 최저가
    private Long stock;     // 재고 수량 (long 타입 주의)
    private String imageUrl;
}
