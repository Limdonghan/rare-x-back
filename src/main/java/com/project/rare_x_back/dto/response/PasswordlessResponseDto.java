package com.project.rare_x_back.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordlessResponseDto {
    private String result;
    private Object data;
    private String sessionId;
    private String oneTimeToken;
    private String pushConnectUrl;
    private String message;
}
