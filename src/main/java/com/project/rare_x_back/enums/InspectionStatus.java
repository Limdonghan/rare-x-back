package com.project.rare_x_back.enums;

public enum InspectionStatus {
    SHIPPED_TO_WAREHOUSE, // 검수센터로 이동 중
    PENDING_INSPECTION,  // 검수 대기
    INSPECTING,          // 검수 중
    PASSED,              // 검수 통과
    FAILED,               // 검수 실패
    RELEASE_REQUESTED,
    RELEASE,
    RELEASE_COMPLETED
}