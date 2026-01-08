package com.project.rare_x_back.dto.request;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.parameters.P;

@Getter
@NoArgsConstructor
public class ProductUpdateRequestDto {
    @Positive
    private Long brandId;

    @Positive
    private Long categoryId;

    private String productName;

    private String productDescription;

    @PositiveOrZero(message = "가격은 0원 이상이어야 합니다.")
    private Integer retailPrice;  //Integer로 둔 이유: PATCH에서 값이 안 오면 null이 들어오기 때문
}
