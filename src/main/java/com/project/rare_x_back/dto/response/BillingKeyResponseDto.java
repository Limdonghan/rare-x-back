package com.project.rare_x_back.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BillingKeyResponseDto {
    private String cardCompany;
    private String cardNumber;
    private boolean hasBillingKey;
}
