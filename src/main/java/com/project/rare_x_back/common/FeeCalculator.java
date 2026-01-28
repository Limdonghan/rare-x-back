package com.project.rare_x_back.common;

public class FeeCalculator {

    private static final double BUYER_FEE_RATE = 0.03;
    private static final double SELLER_FEE_RATE = 0.03;
    private static final int DELIVERY_FEE = 3000;

    public static int buyerFee(int price) {
        return (int) Math.ceil(price * BUYER_FEE_RATE);
    }

    public static int sellerFee(int price) {
        return (int) Math.ceil(price * SELLER_FEE_RATE);
    }

    public static int buyerTotalAmount(int price) {
        return price + buyerFee(price) + DELIVERY_FEE;
    }

    public static int sellerPayout(int price) {
        return price - sellerFee(price);
    }
}
