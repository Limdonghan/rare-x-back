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
    private String productName;
    private long price;
    private BidStatus status;
    private LocalDateTime createdAt;

    public static MySaleBidResponseDto from(SaleBid bid) {
        return MySaleBidResponseDto.builder()
                .bidId(bid.getSellId())
                .productName(bid.getProduct().getProductName())
                .price(bid.getPrice())
                .status(bid.getStatus())
                .createdAt(bid.getCreatedAt())
                .build();
    }
}
