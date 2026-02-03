package com.project.rare_x_back.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class SellingOrderDetailResponseDto {
    private Long orderId;
    private String orderNumber;
    private LocalDateTime createdAt;

    // 상품 정보
    private Long productId;
    private String productName;
    private List<String> productImages;

    // 가격 정보
    private int price;
    private Integer settlementPayout;
    private LocalDateTime settlementCompletedAt;

    // 상태 정보
    private String currentStatus;
    private String returnStatus;

    // 발송 관련
    private LocalDateTime shipDeadline;
    private LocalDateTime sellerShippedAt;

    // 검수 정보
    private String inspectionStatus;
    private String inspectionFailReason;

    // 타임라인
    private List<StatusHistory> statusHistories;

    @Getter
    @Builder
    public static class StatusHistory {
        private String status;
        private LocalDateTime createdAt;
    }
}