package com.project.rare_x_back.dto.response;

import com.project.rare_x_back.entity.BuyBid;
import com.project.rare_x_back.enums.BidStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BuyBidResponseDto {

    private Long buyId;
    private String brandName;
    private String productName;
    private int price;
    private BidStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public static BuyBidResponseDto from(BuyBid buyBid) {
        return BuyBidResponseDto.builder()
                .buyId(buyBid.getBuyId())
                .brandName(buyBid.getProduct().getBrand().getBrandName())
                .productName(buyBid.getProduct().getProductName())
                .price(buyBid.getPrice())
                .status(buyBid.getStatus())
                .createdAt(buyBid.getCreatedAt())
                .expiresAt(buyBid.getExpiresAt())
                .build();
    }
}