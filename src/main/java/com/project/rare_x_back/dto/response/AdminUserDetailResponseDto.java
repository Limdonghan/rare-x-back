package com.project.rare_x_back.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDetailResponseDto {
    // 기본 정보
    private Long userId;
    private String userNumber;
    private String name;
    private String email;
    private String providerType;
    private String status;
    private String profileUrl;
    private LocalDateTime createdAt;

    // 거래 내역
    private long buyOrderCount;
    private long sellOrderCount;

    // 입찰 내역
    private long activeBuyBidCount;
    private long activeSellBidCount;
}