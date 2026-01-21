package com.project.rare_x_back.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Getter
@Builder
@Table(name = "payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column (name = "payment_id")
    private Long paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn (name = "order_id")
    private Order orderId;

    @Column (name = "toss_order_id")
    private String tossOrderId;                     /// 토스 내부에서 관리하는 orderId

    @Column (name = "toss_payment_key", unique = true)
    private String tossPaymentKey;                  /// 토스 결제 키

    @Column (name = "amount")
    private int amount;                             /// 결제 금액

    @Column (name = "method")
    private String method;    /// 결제 방식

    @Column (name = "status")
    private String status;    /// 결제 상태

    /// [추가] 결제 타입 추가 ex) 일반결제, 자동결제
    @Column (name = "type")
    private String type;                 /// 결제 타입

    /// [수정] TossPayment API 시각 받아오기, 타임존 받아오기위해 타입 변경
    @Column (name = "requested_at")
    private OffsetDateTime requestedAt;              /// 결제가 일어난 날짜와 시간 정보

    /// [수정] TossPayment API 시각 받아오기, 타임존 받아오기위해 타입 변경
    @Column (name = "approved_at")
    private OffsetDateTime approvedAt;               /// 결제 승인이 일어난 날짜와 시간 정보

}
