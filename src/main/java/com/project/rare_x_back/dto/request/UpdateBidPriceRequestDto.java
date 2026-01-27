package com.project.rare_x_back.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class UpdateBidPriceRequestDto {
    @NotNull(message = "가격 입력은 필수입니다.")
    private int price;
}
