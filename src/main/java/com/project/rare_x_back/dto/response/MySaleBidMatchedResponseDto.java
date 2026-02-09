package com.project.rare_x_back.dto.response;

import com.project.rare_x_back.entity.Order;
import com.project.rare_x_back.enums.CurrentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MySaleBidMatchedResponseDto {
    private Long orderId;
    private String productName;
    private String brandName;
    private String imageUrl;
    private long price;
    private CurrentStatus currentStatus;
    private LocalDateTime createdAt;
    private LocalDateTime shipDeadline;

    public static MySaleBidMatchedResponseDto from(Order order) {
        String imageUrl = order.getProduct().getImages().isEmpty()
                ? null
                : order.getProduct().getImages().get(0).getImageUrl();

        return MySaleBidMatchedResponseDto.builder()
                .orderId(order.getOrderId())
                .productName(order.getProduct().getProductName())
                .brandName(order.getProduct().getBrand().getBrandName())
                .imageUrl(imageUrl)
                .price(order.getPrice())
                .currentStatus(order.getCurrentStatus())
                .createdAt(order.getCreatedAt())
                .shipDeadline(order.getShipDeadline())
                .build();
    }
}