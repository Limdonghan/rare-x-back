package com.project.rare_x_back.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class WishlistResponseDto {

    // 상품 정보
    private Long productId;
    private String productName;
    private String brandName;
    private String productImageUrl;

    // 가격 정보
    private Integer lowestPrice;  // 즉시 구매가 (null이면 매물 없음)
    private Integer HighestPrice;

    // 찜 정보
    private int wishCount;
    private LocalDateTime createdAt;  // 찜한 시간
}