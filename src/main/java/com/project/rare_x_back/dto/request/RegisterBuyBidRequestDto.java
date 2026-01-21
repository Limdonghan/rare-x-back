package com.project.rare_x_back.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RegisterBuyBidRequestDto {

    @NotNull
    private Long productId;     /// 상품 ID

    @NotNull
    private int price;          /// 구매 가격
}
