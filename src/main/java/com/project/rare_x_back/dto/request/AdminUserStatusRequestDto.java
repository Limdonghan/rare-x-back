package com.project.rare_x_back.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class AdminUserStatusRequestDto {

    private List<Long> userIds;

    @NotBlank(message = "변경할 상태를 입력해주세요.")
    private String status;
}