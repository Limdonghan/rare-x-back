package com.project.rare_x_back.dto.response;

import com.project.rare_x_back.entity.StorageItem;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Getter
@Builder
public class StorageItemResponseDto {

    private Long storageId;
    private Long productId;
    private String productName;
    private String brandName;
    private LocalDateTime storedAt;
    private LocalDateTime expiredAt;
    private long storageDays;           // 보관 일수
    private long accumulatedFee;        // 누적 보관료

    public static StorageItemResponseDto from(StorageItem storageItem) {
        long days = ChronoUnit.DAYS.between(storageItem.getStoredAt(), LocalDateTime.now());
        long fee = Math.round((float) (days * 1000) / 30 / 10) * 10L;  // 월 1,000원 = 일 약 33원, 반올림 적용(10원 단위)

        return StorageItemResponseDto.builder()
                .storageId(storageItem.getStorageId())
                .productId(storageItem.getProduct().getProductId())
                .productName(storageItem.getProduct().getProductName())
                .brandName(storageItem.getProduct().getBrand().getBrandName())
                .storedAt(storageItem.getStoredAt())
                .expiredAt(storageItem.getExpiredAt())
                .storageDays(days)
                .accumulatedFee(fee)
                .build();
    }
}