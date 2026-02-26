package com.project.rare_x_back.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ProductDemandRequestResponseDto {
    private Long userId;
    private Long demandId;
    private String productName;
    private String brandName;
    private Integer retailPrice;
    private String description;
    private String imageUrl;
    private LocalDateTime createdAt;
}
