package com.project.rare_x_back.enums;

public enum ReturnStatus {
    NONE,           // 반송 없음
    HOLD,           // 패널티 미결제로 반송 보류
    REQUESTED,      // 반송 요청됨
    IN_PROGRESS,    // 반송 중
    COMPLETED       // 반송 완료
}