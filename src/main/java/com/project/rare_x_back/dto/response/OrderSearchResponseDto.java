package com.project.rare_x_back.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderSearchResponseDto {
    private Long orderId;
    private String buyerName;
    private String sellerName;
    private String productName;
    private int price;
    private String currentStatus;
}