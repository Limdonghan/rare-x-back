package com.project.rare_x_back.entity;

import com.project.rare_x_back.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "settlements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Settlement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long settlementId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sell_id")
    private SaleBid sellBid;

    @Column(name = "total_price", nullable = false)
    private int totalPrice;        // 주문 총액

    @Column(name = "commission_fee", nullable = false)
    private int commissionFee;     // 1.2% 수수료

    @Column(name = "delivery_fee", nullable = false)
    private int deliveryFee;       // 배송비

    @Column(name = "payout", nullable = false)
    private int payout;            // 판매자 지급액

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementStatus status;   // PENDING, COMPLETED, FAILED

    @Column(name = "fail_reason")
    private String failReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

}
