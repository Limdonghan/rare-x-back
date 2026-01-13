package com.project.rare_x_back.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Getter @Setter
@Builder
@Table(name = "payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column (name = "payment_id")
    private Long paymentId;

    /// TODO 주문 테이블 완성 후 매핑 할 것
    //@ManyToOne(fetch = FetchType.LAZY)
//    @Column (name = "order_id")
//    private Long orderId;

    @Column (name = "order_id") // 임시로 ID만 저장
    private Long orderId;

    @Column (name = "toss_order_id")
    private String tossOrderId;                     /// 토스 내부에서 관리하는 orderId

    @Column (name = "toss_payment_key")
    private String tossPaymentKey;                  /// 토스 결제 키

    @Column (name = "amount")
    private int amount;                             /// 결제 금액

//    @Enumerated(EnumType.STRING)
    @Column (name = "toss_payment_method")
    private String tossPaymentMethod;    /// 결제 방식

//    @Enumerated(EnumType.STRING)
    @Column (name = "status")
    private String tossPaymentStatus;    /// 결제 상태

    /// [수정] TossPayment API 시각 받아오기, 타임존 받아오기위해 타입 변경
    @Column (name = "approved_at")
    private OffsetDateTime approvedAt;               /// 결제 요청 시기

    /// [수정] TossPayment API 시각 받아오기, 타임존 받아오기위해 타입 변경
    @Column (name = "requested_at")
    private OffsetDateTime requestedAt;              /// 결제 승인 시간


}
