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
public class CategoryListResponseDto {
    private Long categoryId;      // 카테고리 고유 식별 ID
    private String categoryName;  // 카테고리 명칭 (예: 피규어, 레고 등)
    private Long productCount;    // 해당 카테고리에 속한 활성화된 상품의 총 개수
    private LocalDateTime createdAt; // 카테고리 생성 일시
}
