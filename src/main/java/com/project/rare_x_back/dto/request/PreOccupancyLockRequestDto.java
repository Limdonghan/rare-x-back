package com.project.rare_x_back.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreOccupancyLockRequestDto {
    @NotNull(message = "상품 ID는 필수입니다.")
    private Long productId;
    
    @NotNull(message = "구매 가격은 필수입니다.")
    private Integer price;
}
