package com.project.rare_x_back.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
@NotNull
public class RegisterBuyBidRequestDto {
    private Long productId;     /// 상품 ID
    private int price;          /// 구매 가격
}
