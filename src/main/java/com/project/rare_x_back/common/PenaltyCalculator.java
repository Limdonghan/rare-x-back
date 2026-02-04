package com.project.rare_x_back.common;

public class PenaltyCalculator {
    // 구매자 취소 패널티: 결제금액의 5%
    public static int cancelPenalty(int paymentAmount) {
        return (int) Math.ceil(paymentAmount * 0.05);
    }
}
