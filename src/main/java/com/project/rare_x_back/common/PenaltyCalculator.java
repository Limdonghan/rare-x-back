package com.project.rare_x_back.common;

public class PenaltyCalculator {
    // 주문 취소 패널티 -> 결제 금액의 5%
    public static long cancelPenalty(long paymentAmount) {
        return Math.round(paymentAmount * 0.05);  //소수점 내임
    }
}
