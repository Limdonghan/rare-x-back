package com.project.rare_x_back.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SellingOrderResponseDto {
    private Long orderId;
    private String orderNumber;
    private LocalDateTime createdAt;

    // 상품 정보
    private Long productId;
    private String productName;
    private String productImage;

    // 가격 정보
    private int price;                      // 판매가
    private Integer settlementPayout;       // 정산 예정 금액 (null일 수 있음)

    // 상태 정보
    private String currentStatus;
    private String returnStatus;            // 반송 상태

    // 발송 관련
    private LocalDateTime shipDeadline;     // 발송 마감 기한

    // 검수 정보 (실패 시)
    private String inspectionFailReason;
}