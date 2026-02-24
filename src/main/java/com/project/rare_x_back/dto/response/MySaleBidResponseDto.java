package com.project.rare_x_back.dto.response;

import com.project.rare_x_back.entity.SaleBid;
import com.project.rare_x_back.enums.BidStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MySaleBidResponseDto {
    private Long bidId;
    private Long productId;
    private String productName;
    private String brandName;
    private String imageUrl;
    private long price;
    private BidStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public static MySaleBidResponseDto from(SaleBid bid) {
        String imageUrl = bid.getProduct().getImages().isEmpty()
                ? null
                : bid.getProduct().getImages().get(0).getImageUrl();

        return MySaleBidResponseDto.builder()
                .bidId(bid.getSellId())
                .productId(bid.getProduct().getProductId())
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
