package com.project.rare_x_back.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryListResponseDto {
    private Long categoryId;
    private String categoryName;
}
