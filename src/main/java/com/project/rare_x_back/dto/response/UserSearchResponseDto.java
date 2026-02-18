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
public class UserSearchResponseDto {
    private Long userId;
    private String email;
    private String name;
    private String role;
    private String status;
    private String providerType;
    private LocalDateTime createdAt;
}