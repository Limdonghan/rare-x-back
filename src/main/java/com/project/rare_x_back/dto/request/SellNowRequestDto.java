package com.project.rare_x_back.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SellNowRequestDto {

    private Long bidId;

    @NotNull
    private Long productId;     /// 판매할 상품
    @NotNull
    private Integer price;      /// 즉시 판매 가격 (최고가 매수 입찰가와 같아야 함)
}
