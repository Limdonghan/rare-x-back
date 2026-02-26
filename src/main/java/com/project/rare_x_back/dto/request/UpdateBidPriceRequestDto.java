package com.project.rare_x_back.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

@Getter
public class UpdateBidPriceRequestDto {
    @NotNull(message = "가격 입력은 필수입니다.")
    @Positive(message = "입찰 가격은 0보다 커야 합니다.")
    @Min(value = 1000, message = "입찰 가격은 1000원 이상이어야 합니다.")
    private int price;
}
