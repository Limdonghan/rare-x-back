package com.project.rare_x_back.dto.response;

import com.project.rare_x_back.entity.StorageRequest;
import com.project.rare_x_back.enums.StorageRequestStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class StorageRequestResponseDto {

    private Long storageRequestId;
    private Long productId;
    private String productName;
    private String brandName;
    private StorageRequestStatus status;
    private LocalDateTime createdAt;

    public static StorageRequestResponseDto from(StorageRequest storageRequest) {
        return StorageRequestResponseDto.builder()
                .storageRequestId(storageRequest.getStorageRequestId())
                .productId(storageRequest.getProduct().getProductId())
                .productName(storageRequest.getProduct().getProductName())
                .brandName(storageRequest.getProduct().getBrand().getBrandName())
                .status(storageRequest.getStatus())
                .createdAt(storageRequest.getCreatedAt())
                .build();
    }
}
