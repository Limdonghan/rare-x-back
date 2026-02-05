package com.project.rare_x_back.enums;

public enum WalletHistoryType {
    SETTLEMENT,     // 정산 지급
    COMPENSATION,   // 패널티 보상(거래실패 보상 -> 상대방에게 징수한 패널티 / 2)
    PENALTY_DECREASE   // 패넕티 차감
}
