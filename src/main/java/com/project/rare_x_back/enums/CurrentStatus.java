package com.project.rare_x_back.enums;

public enum CurrentStatus {
    PENDING,              ///  판매완료, 발송 전
    SHIPPED_TO_WAREHOUSE, /// 검수센터로 발송됨
    PENDING_INSPECTION,   /// 검수 대기 (판매자가 물건을 보냈고, 센터에 도착해서 순서를 기다리는 중)
    INSPECTING,           /// 검수중 (전문가가 정품 여부와 상태를 확인하는 중)
    PASSED,               /// 검수 합격 (정품 확인 완료. 곧 배송 시작됨)
    SHIPPED,              /// 배송중 (검수 합격한 상품을 구매자에게 발송함)
    DELIVERED,            /// 배송 완료 (구매자가 상품을 수령함)
    RETURN,               /// 반송/검수 탈락 (가품이거나 상태가 안 좋아서 판매자에게 돌려보냄)
    CANCELLED             /// 취소됨 (판매자가 발송을 안 했거나, 사용자가 취소함)
}
