package com.project.rare_x_back.entity;

import com.project.rare_x_back.enums.PaymentHistoryStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@Table(name = "payment_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PaymentHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_history_id")
    private Long paymentHistoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buy_id")
    private BuyBid buyBid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @Column(name = "commission_fee", nullable = false)
    private int commissionFee;

    @Column(name = "total_amount", nullable = false)
    private int totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentHistoryStatus status;

    @Column(name = "req_date", updatable = false)
    @CreationTimestamp
    private LocalDateTime reqDate;

    @Column(name = "res_date")
    private LocalDateTime resDate;

    @Column(name = "delivery_fee")
    private int deliveryFee;

    // 상태 변경 편의 메서드
    public void complete() {
        this.status = PaymentHistoryStatus.COMPLETE;
        this.resDate = LocalDateTime.now();
    }

    public void fail() {
        this.status = PaymentHistoryStatus.FAILED;
        this.resDate = LocalDateTime.now();
    }



}
