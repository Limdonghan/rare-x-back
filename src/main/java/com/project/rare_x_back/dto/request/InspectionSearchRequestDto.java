package com.project.rare_x_back.dto.request;

import com.project.rare_x_back.enums.InspectionStatus;
import com.project.rare_x_back.enums.InspectionType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
public class InspectionSearchRequestDto {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDate;      // 검수 완료일 시작

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endDate;        // 검수 완료일 종료

    private InspectionStatus status;      // PASSED / FAILED
    private Long inspectorId;             // 검수자 ID
    private Long categoryId;              // 상품 카테고리 ID
    private InspectionType type;          // STORAGE / ORDER
}