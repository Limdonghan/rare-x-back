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
    @Min(value = 1000, message = "구매 입찰 가격은 1000원 이상이어야합니다.")
    private int price;          /// 구매 가격

    @NotNull
    private Long addressId;
}
