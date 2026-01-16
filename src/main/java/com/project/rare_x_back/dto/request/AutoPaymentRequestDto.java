package com.project.rare_x_back.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AutoPaymentRequestDto {

    @NotBlank
    private String orderId;     /// 주문 ID

    @NotBlank
    private String orderName;   /// 주문명

    @NotBlank
    private String paymentKey;

    @NotBlank
    private int amount;         /// 결제 금액
}
