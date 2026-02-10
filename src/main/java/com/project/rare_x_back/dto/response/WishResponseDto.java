package com.project.rare_x_back.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WishResponseDto {

    private int wishCount;  // 현재 관심상품 찜 수
}
