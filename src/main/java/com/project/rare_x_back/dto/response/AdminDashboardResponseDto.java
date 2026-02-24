package com.project.rare_x_back.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminDashboardResponseDto {
    private String date;
    private long todayRevenue;                // 오늘 플랫폼 수익(수수료+패널티-보상)
    private long todayOrderCount;             // 오늘 주문 수
    private long pendingInspectionCount; // 검수 대기 수
    private long activeUserCount;        // ACTIVE 회원 수

}
