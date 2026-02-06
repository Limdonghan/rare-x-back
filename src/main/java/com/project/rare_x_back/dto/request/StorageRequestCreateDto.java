package com.project.rare_x_back.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StorageRequestCreateDto {

    @NotNull(message = "상품 ID를 입력해주세요")
    private Long productId;
}
