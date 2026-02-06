package com.project.rare_x_back.entity;

import com.project.rare_x_back.enums.CancelStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
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
    private Order order;

    @Column (name = "toss_order_id")
    private String tossOrderId;                     /// 토스 내부에서 관리하는 orderId

    @Column (name = "toss_payment_key", unique = true)
    private String tossPaymentKey;                  /// 토스 결제 키

    /// [추가] 토스에 보낸 멱등키 저장 (디버깅 및 추적용)
    @Column(name = "idempotency_key")
    private String idempotencyKey;

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

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "cancel_status", nullable = false)
    private CancelStatus cancelStatus = CancelStatus.NONE;  // 취소 관련 상태

    @Builder.Default
    @Column(name = "canceled_amount", nullable = false)
    private long canceledAmount = 0; //환불(취소)된 금액

    @Column(name = "last_canceled_at")
    private LocalDateTime lastCanceledAt; //환불 성공 시점



    // 취소 요청이 들어왔음을 표시 (락 잡은 상태에서 호출) + [추가] 멱등키 같이 저장
    public void markCancelRequested(String idempotencyKey) {
        this.cancelStatus = CancelStatus.REQUESTED;
        this.idempotencyKey = idempotencyKey;
    }

    // 취소 성공 시 호출
    public void applyCancelSuccess(long cancelAmount) {
        this.canceledAmount = cancelAmount;

        // 전액, 부분 취소 구분
        if (cancelAmount == this.amount) {
            this.cancelStatus = CancelStatus.CANCELED;
        } else {
            this.cancelStatus = CancelStatus.PARTIAL_CANCELED;
        }

        this.lastCanceledAt = LocalDateTime.now();
    }

    // 토스 취소 실패 시 호출
    public void markCancelFailed() {
        this.cancelStatus = CancelStatus.CANCEL_FAILED;
    }

}
