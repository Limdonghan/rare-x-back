package com.project.rare_x_back.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrandListResponseDto {
    private Long brandId;         // 브랜드 고유 식별 ID
    private String brandName;     // 브랜드 명칭 (예: NIKE, BE@RBRICK 등)
    private Long productCount;    // 해당 브랜드로 등록된 활성화된 상품의 총 개수
    private LocalDateTime createdAt; // 브랜드 등록 일시
}
