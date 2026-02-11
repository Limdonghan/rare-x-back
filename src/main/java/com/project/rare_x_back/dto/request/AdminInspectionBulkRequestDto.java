package com.project.rare_x_back.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class AdminInspectionBulkRequestDto {

    @NotEmpty(message = "검수 ID 목록은 필수입니다.")
    private List<Long> inspectionIds;

    // 합격 시 체크리스트 (기존 DTO 재사용)
    private InspectionChecklistRequestDto checklist;

    // 불합격 시 사유 (기존 DTO 필드 재사용 안 하고 단순 String)
    private String failReason;
}