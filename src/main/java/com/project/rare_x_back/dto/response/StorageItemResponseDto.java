package com.project.rare_x_back.dto.response;

import com.project.rare_x_back.common.FeeCalculator;
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
    private String status;

    public static StorageItemResponseDto from(StorageItem storageItem) {
        long days = ChronoUnit.DAYS.between(storageItem.getStoredAt(), LocalDateTime.now());
        long fee = calculateFee(days);

        return StorageItemResponseDto.builder()
                .storageId(storageItem.getStorageId())
                .productId(storageItem.getProduct().getProductId())
                .productName(storageItem.getProduct().getProductName())
                .brandName(storageItem.getProduct().getBrand().getBrandName())
                .storedAt(storageItem.getStoredAt())
                .expiredAt(storageItem.getExpiredAt())
                .storageDays(days)
                .accumulatedFee(fee)
                .status(storageItem.getStatus().name())
                .build();
    }

    private static long calculateFee(long totalDays) {
        if (totalDays <= totalDays - FeeCalculator.STORAGE_FREE_DAYS) {
            return 0;
        }

        long paidDays = totalDays - FeeCalculator.STORAGE_FREE_DAYS;
        long months = (long) Math.ceil((double) paidDays / 30);
        return months * FeeCalculator.STORAGE_FEE_PER_MONTH;
    }
}