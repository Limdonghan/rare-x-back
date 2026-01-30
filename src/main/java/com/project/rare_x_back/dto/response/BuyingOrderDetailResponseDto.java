package com.project.rare_x_back.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class BuyingOrderDetailResponseDto {
    // 주문 정보
    private Long orderId;
    private String orderNumber;
    private LocalDateTime createdAt;
    private String currentStatus;

    // 상품 정보
    private Long productId;
    private String productName;
    private List<String> productImages;

    // 결제 정보
    private int productPrice;
    private int shippingFee;
    private int totalAmount;
    private String cardCompany;
    private String cardNumberLast4;

    // 배송지 정보
    private String recipientName;
    private String postalCode;
    private String address;
    private String detailAddress;

    // 검수 정보 (실패 시에만)
    private String inspectionStatus;
    private String failReason;

    // 상태 이력 (주문상세에서 배송조회)
    private List<StatusHistory> statusHistories;

    // Inner Class
    @Getter
    @Builder
    public static class StatusHistory {
        private String status;
        private LocalDateTime createdAt;
    }
}