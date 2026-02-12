package com.project.rare_x_back.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SellNowResponseDto {

    private String productName;
    private String brandName;
    private String category;

    private String tossOrderId;
    private int amount;
}
