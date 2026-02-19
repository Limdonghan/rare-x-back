package com.project.rare_x_back.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class RegisterBuyBidResponseDto {
    private Long bidId;
    private String productName;     /// 상품이름
    private String brandName;       /// 브랜드이름
    private String category;        /// 카테고리
    private int price;              /// 등록 가격
}
