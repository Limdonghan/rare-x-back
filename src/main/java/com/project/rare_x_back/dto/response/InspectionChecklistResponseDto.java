package com.project.rare_x_back.dto.response;

import com.project.rare_x_back.entity.InspectionChecklist;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InspectionChecklistResponseDto {
    private Long checklistId;
    private Long inspectionId;
    private Boolean isAuthentic;          // 정품 여부
    private Boolean isExteriorGood;       // 외관 상태 양호
    private Boolean isComponentsComplete; // 구성품 완비
    private Boolean isPackagingGood;      // 포장 상태 양호
    private Boolean isUnused;             // 미사용 여부

    public static InspectionChecklistResponseDto from(InspectionChecklist checklist) {
        return InspectionChecklistResponseDto.builder()
                .checklistId(checklist.getChecklistId())
                .inspectionId(checklist.getInspection().getInspectionId())
                .isAuthentic(checklist.getIsAuthentic())
                .isExteriorGood(checklist.getIsExteriorGood())
                .isComponentsComplete(checklist.getIsComponentsComplete())
                .isPackagingGood(checklist.getIsPackagingGood())
                .isUnused(checklist.getIsUnused())
                .build();
    }
}