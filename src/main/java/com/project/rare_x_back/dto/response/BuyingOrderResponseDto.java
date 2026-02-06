package com.project.rare_x_back.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BuyingOrderResponseDto {
    private Long orderId;
    private String orderNumber;
    private LocalDateTime createdAt;

    // 상품 정보
    private Long productId;
    private String productName;
    private String productImage;

    // 금액
    private int price;

    // 상태
    private String currentStatus;
    private String inspectionStatus;
    private String inspectionFailReason;
}
