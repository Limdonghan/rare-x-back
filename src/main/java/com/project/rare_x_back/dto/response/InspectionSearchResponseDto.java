package com.project.rare_x_back.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InspectionSearchResponseDto {
    private Long inspectionId;
    private String productName;
    private String sellerName;
    private String inspectorName;
    private String type;
    private String status;
    private LocalDateTime createdAt;
}