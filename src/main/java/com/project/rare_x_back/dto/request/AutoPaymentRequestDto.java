package com.project.rare_x_back.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AutoPaymentRequestDto {
    private String orderId;     /// 주문 ID
    private String orderName;   /// 주문명
    private int amount;         /// 결제 금액
}
