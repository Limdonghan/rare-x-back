package com.project.rare_x_back.dto.response;

import com.project.rare_x_back.entity.ProductImage;
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
    private String productImageUrl;
    private StorageRequestStatus status;
    private LocalDateTime createdAt;

    // 검수센터 정보
    private String inspectionCenterAddress;
    private String inspectionCenterZipcode;

    public static StorageRequestResponseDto from(StorageRequest storageRequest,
                                                 String inspectionCenterAddress,
                                                 String inspectionCenterZipcode) {
        // 첫 번째 상품 이미지 URL
        String imageUrl = storageRequest.getProduct().getImages().stream()
                .findFirst()
                .map(ProductImage::getImageUrl)
                .orElse(null);

        return StorageRequestResponseDto.builder()
                .storageRequestId(storageRequest.getStorageRequestId())
                .productId(storageRequest.getProduct().getProductId())
                .productName(storageRequest.getProduct().getProductName())
                .brandName(storageRequest.getProduct().getBrand().getBrandName())
                .productImageUrl(imageUrl)
                .status(storageRequest.getStatus())
                .createdAt(storageRequest.getCreatedAt())
                .inspectionCenterAddress(inspectionCenterAddress)
                .inspectionCenterZipcode(inspectionCenterZipcode)
                .build();
    }
}