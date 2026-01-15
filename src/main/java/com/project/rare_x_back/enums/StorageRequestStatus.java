package com.project.rare_x_back.enums;

public enum StorageRequestStatus {
    PENDING_INSPECTION,  // 검수 대기
    INSPECTING,          // 검수 중
    PASSED,              // 검수 통과
    FAILED,              // 검수 실패
    SHIPPED,             // 발송됨
    DELIVERED,           // 배송 완료
    RETURN,              // 반송
    CANCELLED            // 취소
}

