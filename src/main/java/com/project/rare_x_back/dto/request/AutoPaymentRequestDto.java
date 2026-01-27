package com.project.rare_x_back.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutoPaymentRequestDto {

    @NotNull
    private Long userId;

    @NotBlank
    private String tossOrderId;     /// 주문 ID

    @NotBlank
    private String orderName;   /// 주문명

    @NotBlank
    private String paymentKey;

    @NotBlank
    private int amount;         /// 결제 금액

    private Long orderId;
}
