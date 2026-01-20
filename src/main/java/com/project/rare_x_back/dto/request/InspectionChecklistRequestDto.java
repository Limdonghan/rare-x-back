package com.project.rare_x_back.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InspectionChecklistRequestDto {

    @NotNull(message = "정품 여부는 필수입니다")
    private Boolean isAuthentic;          // 정품 여부

    @NotNull(message = "외관 상태는 필수입니다")
    private Boolean isExteriorGood;       // 외관 상태 양호

    @NotNull(message = "구성품 완비 여부는 필수입니다")
    private Boolean isComponentsComplete; // 구성품 완비

    @NotNull(message = "포장 상태는 필수입니다")
    private Boolean isPackagingGood;      // 포장 상태 양호

    @NotNull(message = "미사용 여부는 필수입니다")
    private Boolean isUnused;             // 미사용 여부
}