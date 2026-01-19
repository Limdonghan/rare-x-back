package com.project.rare_x_back.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InspectionChecklistRequestDto {
    private Boolean isAuthentic;          // 정품 여부
    private Boolean isExteriorGood;       // 외관 상태 양호
    private Boolean isComponentsComplete; // 구성품 완비
    private Boolean isPackagingGood;      // 포장 상태 양호
    private Boolean isUnused;             // 미사용 여부
}