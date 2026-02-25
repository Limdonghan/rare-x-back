package com.project.rare_x_back.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductDemandRequestDto {

    @NotBlank
    private String productName;

    @NotBlank
    private String brandName;

    @NotNull
    private Integer retailPrice;

    @NotBlank
    private String description;
}
