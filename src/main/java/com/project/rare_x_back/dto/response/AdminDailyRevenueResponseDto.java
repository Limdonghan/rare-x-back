package com.project.rare_x_back.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminDailyRevenueResponseDto {
    private String date;     // yyyy-MM-dd
    private long revenue;    // 그날 매출
}
