package com.project.rare_x_back.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder
@Getter
public class ProductDemandRequestDetailResponseDto {
    private Long demandId;
    private String productName;
    private String brandName;
    private Integer retailPrice;
    private String imageUrl;
    private String description;
    private Long userId;
    private LocalDateTime createdAt;
}
