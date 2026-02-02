package com.project.rare_x_back.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRequestDto {
    private Long productId;     /// 상품 ID
    private int price;          /// 구매 가격

    private Long addressId;     // 구매자가 선택한 배송지Id

    private String paymentKey;  /// 토스페이먼츠 키
    private String tossOrderId; /// 토스 주문 ID
    private int amount;         /// 토스 실제 금액

    private Long address;
}
