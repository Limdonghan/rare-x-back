package com.project.rare_x_back.dto.response;

import com.project.rare_x_back.common.ApiResponse;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;

@Data
@NoArgsConstructor
public class PasswordlessStatusResponseDto {
    private boolean exist;

    public static final ParameterizedTypeReference<ApiResponse<PasswordlessStatusResponseDto>> responseType = new ParameterizedTypeReference<>() {};
}
