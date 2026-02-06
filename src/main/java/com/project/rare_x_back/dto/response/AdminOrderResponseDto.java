package com.project.rare_x_back.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminOrderResponseDto {
    private Long orderId;
    private String orderNumber;
    private LocalDateTime createdAt;
    private String buyerName;
    private String sellerName;
    private String productName;
    private int price;
    private String currentStatus;
}