package com.project.rare_x_back.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeResponseDto {
    private double buyerFeeRate;
    private double sellerFeeRate;
    private int deliveryFee;
}
