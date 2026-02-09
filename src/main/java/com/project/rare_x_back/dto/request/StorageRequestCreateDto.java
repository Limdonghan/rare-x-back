package com.project.rare_x_back.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StorageRequestCreateDto {

    @NotNull(message = "상품 ID를 입력해주세요")
    private Long productId;

    @Min(value = 1, message = "최소 1개 이상 신청해야 합니다")
    @Max(value = 10, message = "최대 10개까지 신청 가능합니다")
    private Integer quantity = 1;
}
