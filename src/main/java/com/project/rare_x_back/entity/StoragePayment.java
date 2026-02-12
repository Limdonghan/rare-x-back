package com.project.rare_x_back.entity;

import com.project.rare_x_back.enums.StoragePaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@Table(name = "storage_payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class StoragePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "storage_payment_id")
    private Long storagePaymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storage_id", nullable = false)
    private StorageItem storageItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder.Default
    @Column(name = "amount", nullable = false)
    private int amount = 3000;      // 결제 금액

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StoragePaymentStatus status = StoragePaymentStatus.PENDING;

    @Builder.Default
    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;     // 자동결제 재시도 횟수

    @Column(name = "billing_period_start", nullable = false)
    private LocalDate billingPeriodStart;   // 과금 기간 시작일

    @Column(name = "billing_period_end", nullable = false)
    private LocalDate billingPeriodEnd;     // 과금 기간 종료일

    @Column(name = "toss_payment_key")
    private String tossPaymentKey;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // === 비즈니스 메서드 ===

    /**
     * 결제 성공 처리
     */
    public void markSuccess(String tossPaymentKey) {
        this.status = StoragePaymentStatus.SUCCESS;
        this.tossPaymentKey = tossPaymentKey;
        this.paidAt = LocalDateTime.now();
    }

    /**
     * 결제 실패 처리 (재시도 횟수 증가)
     */
    public void markFailed() {
        this.status = StoragePaymentStatus.FAILED;
        this.retryCount++;
    }

    /**
     * 재시도 가능 여부 (3회 미만)
     */
    public boolean canRetry() {
        return this.retryCount < 3;
    }

    /**
     * 재시도를 위해 PENDING으로 초기화
     */
    public void resetForRetry() {
        this.status = StoragePaymentStatus.PENDING;
    }
}