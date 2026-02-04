package com.project.rare_x_back.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;


@Entity
@Table(name = "payment_cancels")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PaymentCancel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentCancelId;

    // 어떤 결제에 대한 취소인지 (결제 1건당 취소는 1번만 → DB 유니크로 보장)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    // 실제 환불 금액
    @Column(name = "cancel_amount")
    private long cancelAmount;

    // BUYER_CANCELED / SELLER_CANCELED / INSPECTION_FAILED 등..
    @Column(name = "cancel_reason")
    private String cancelReason;

    // BUYER / SELLER / SYSTEM / INSPECTION
    @Column(name = "requested_by")
    private String requestedBy;

    // 토스에서 환불 성공한 시각
    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
