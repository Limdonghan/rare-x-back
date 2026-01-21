package com.project.rare_x_back.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PurchaseRequestDto {
    private Long productId;     /// 상품 ID
    private Long orderId;       /// 주문 ID
    private int price;          /// 구매 가격


    private String paymentKey;  /// 토스페이먼츠 키
    private String tossOrderId; /// 토스 주문 ID
    private int amount;         /// 토스 실제 금액
}
