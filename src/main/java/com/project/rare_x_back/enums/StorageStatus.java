package com.project.rare_x_back.enums;

public enum StorageStatus {
    STORED, // 보관중
    ON_SALE, // 입찰중
    SOLD, // 판매완료
    RELEASED, // 출고완료
    RETURNED,    // 반송 출고 (고객 요청)
    SUSPENDED    // 결제 미납 판매중지
}
