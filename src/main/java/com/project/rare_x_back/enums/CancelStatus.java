package com.project.rare_x_back.enums;

public enum CancelStatus {
    NONE, //아직 취소 시도 없음
    REQUESTED, //취소 처리 중
    PARTIAL_CANCELED, //부분 취소 완료
    CANCELED, //전액 취소 완료
    CANCEL_FAILED //취소 시도했으나 실패
}
