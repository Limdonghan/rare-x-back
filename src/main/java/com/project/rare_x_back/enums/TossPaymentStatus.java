package com.project.rare_x_back.enums;

public enum TossPaymentStatus {
    READY,                  /// 초기상태
    IN_PROGRESS,            /// 결제 수단 인증 중
    WAITING_FOR_DEPOSIT,    /// 가상계좌 입금 대기
    DONE,                   /// 결제 완료
    CANCELED,               /// 전체 취소
    PARTIAL_CANCELED,       /// 부분 취소
    ABORTED,                /// 결제 승인 실패
    EXPIRED                 /// 결제 유효 시간 만료
}
