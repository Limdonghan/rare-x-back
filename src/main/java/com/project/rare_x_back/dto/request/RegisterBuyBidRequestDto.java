package com.project.rare_x_back.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RegisterBuyBidRequestDto {

    @NotNull
    private Long productId;     /// 상품 ID

    @NotNull
    @Min(1000)
    private int price;          /// 구매 가격

    @NotNull
    private Long addressId;
}
