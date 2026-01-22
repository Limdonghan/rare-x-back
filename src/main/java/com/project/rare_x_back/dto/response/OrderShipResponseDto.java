package com.project.rare_x_back.dto.response;

import com.project.rare_x_back.entity.Order;
import com.project.rare_x_back.enums.CurrentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class OrderShipResponseDto {

    private Long orderId;
    private Long productId;
    private String productName;
    private String brandName;
    private int price;
    private CurrentStatus currentStatus;
    private LocalDateTime createdAt;

    // 검수센터 정보
    private String inspectionCenterAddress;
    private String inspectionCenterZipcode;

    public static OrderShipResponseDto from(Order order,
                                            String inspectionCenterAddress,
                                            String inspectionCenterZipcode) {
        return OrderShipResponseDto.builder()
                .orderId(order.getOrderId())
                .productId(order.getProduct().getProductId())
                .productName(order.getProduct().getProductName())
                .brandName(order.getProduct().getBrand().getBrandName())
                .price(order.getPrice())
                .currentStatus(order.getCurrentStatus())
                .createdAt(order.getCreatedAt())
                .inspectionCenterAddress(inspectionCenterAddress)
                .inspectionCenterZipcode(inspectionCenterZipcode)
                .build();
    }
}