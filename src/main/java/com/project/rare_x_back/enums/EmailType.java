package com.project.rare_x_back.enums;

public enum EmailType {
    VERIFICATION,   // 이메일 인증
    TEMP_PASSWORD,   // 임시 비밀번호
    NOTIFICATION,    // 알림 메일 (기본)
    INSPECTION_RESULT, // 검수 결과
    PURCHASE_BID_MATCHED, // 구매 입찰 체결
    SALE_BID_MATCHED // 판매 입찰 체결
}
