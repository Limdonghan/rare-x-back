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
    private long price;
    private BidStatus status;
    private LocalDateTime createdAt;

    public static MyBuyBidResponseDto from(BuyBid bid) {
        return MyBuyBidResponseDto.builder()
                .bidId(bid.getBuyId())
                .productName(bid.getProduct().getProductName())
                .price(bid.getPrice())
                .status(bid.getStatus())
                .createdAt(bid.getCreatedAt())
                .build();
    }
}
