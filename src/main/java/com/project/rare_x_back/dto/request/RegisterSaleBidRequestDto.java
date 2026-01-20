package com.project.rare_x_back.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RegisterSaleBidRequestDto {
    private Long productId;     /// 상품 ID
    private int price;          /// 판매 가격

}
