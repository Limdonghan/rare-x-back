package com.project.rare_x_back.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InspectionFailRequestDto {

    @NotBlank(message = "불합격 사유는 필수입니다")
    private String failReason;
}
