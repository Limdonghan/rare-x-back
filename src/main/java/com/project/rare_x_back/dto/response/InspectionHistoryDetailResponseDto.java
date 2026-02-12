package com.project.rare_x_back.dto.response;

import com.project.rare_x_back.entity.Inspection;
import com.project.rare_x_back.entity.InspectionChecklist;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InspectionHistoryDetailResponseDto {

    private InspectionHistoryResponseDto basicInfo;
    private InspectionChecklistResponseDto checklist;

    public static InspectionHistoryDetailResponseDto from(Inspection inspection, InspectionChecklist checklist) {
        return InspectionHistoryDetailResponseDto.builder()
                .basicInfo(InspectionHistoryResponseDto.from(inspection))
                .checklist(checklist != null ? InspectionChecklistResponseDto.from(checklist) : null)
                .build();
    }
}