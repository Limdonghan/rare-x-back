package com.project.rare_x_back.dto.response;

import com.project.rare_x_back.entity.BuyBid;
import com.project.rare_x_back.enums.BidStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MyBuyBidResponseDto {
    private Long bidId;
    private String productName;
    private String brandName;
    private String imageUrl;
    private long price;
    private BidStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public static MyBuyBidResponseDto from(BuyBid bid) {
        String imageUrl = bid.getProduct().getImages().isEmpty()
                ? null
                : bid.getProduct().getImages().get(0).getImageUrl();

        return MyBuyBidResponseDto.builder()
                .bidId(bid.getBuyId())
                .productName(bid.getProduct().getProductName())
                .brandName(bid.getProduct().getBrand().getBrandName())
                .imageUrl(imageUrl)
                .price(bid.getPrice())
                .status(bid.getStatus())
                .createdAt(bid.getCreatedAt())
                .expiresAt(bid.getExpiresAt())
                .build();
    }
}
