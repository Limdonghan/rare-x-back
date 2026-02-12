package com.project.rare_x_back.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;



@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageProductResponseDto {
    private Long productId;
    private String brandName;
    private String productName;
    private int price;      // 최저가
    private Long stock;     // 재고 수량 (long 타입 주의)
    private String imageUrl;

    public StorageProductResponseDto(Long productId, String brandName, String productName, int price, Long stock) {
        this.productId = productId;
        this.brandName = brandName;
        this.productName = productName;
        this.price = price;
        this.stock = stock;
    }
}
