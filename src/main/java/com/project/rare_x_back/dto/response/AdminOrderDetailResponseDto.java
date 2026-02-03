package com.project.rare_x_back.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class AdminOrderDetailResponseDto {
    // 기본 정보
    private Long orderId;
    private String orderNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 구매자 정보
    private String buyerName;
    private String buyerEmail;

    // 판매자 정보
    private String sellerName;
    private String sellerEmail;

    // 상품 정보
    private Long productId;
    private String productName;
    private List<String> productImages;
    private String brandName;

    // 거래 정보
    private int price;
    private String bidType;
    private String currentStatus;
    private String returnStatus;

    // 발송 정보
    private LocalDateTime shipDeadline;
    private LocalDateTime sellerShippedAt;

    // 결제 정보
    private String paymentMethod;
    private Integer paymentAmount;
    private String paymentStatus;

    // 정산 정보
    private Integer settlementPayout;
    private String settlementStatus;
    private LocalDateTime settlementCompletedAt;

    // 배송지 정보
    private String recipientName;
    private String postalCode;
    private String address;
    private String detailAddress;

    // 검수 정보
    private String inspectionStatus;
    private String inspectionFailReason;

    // 상태 이력
    private List<StatusHistory> statusHistories;

    @Getter
    @Builder
    public static class StatusHistory {
        private String status;
        private LocalDateTime createdAt;
    }
}