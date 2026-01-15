package com.project.rare_x_back.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PaymentConfirmRequestDto {
    private String paymentKey;      /// 토스가 발급한 결제 키
    private String orderId;         /// 우리가 만든 주문 ID (toss_order_id)
    private int amount;             /// 결제 금액

}
