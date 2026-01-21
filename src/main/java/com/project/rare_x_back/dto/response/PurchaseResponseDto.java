package com.project.rare_x_back.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class PurchaseResponseDto {
    private String productName;     /// 상품이름
    private String brandName;       /// 브랜드이름
    private String category;        /// 카테고리

    private String tossOrderId;     /// 토스 주문 번호
    private int amount;         /// 토스 실제 금액
}
