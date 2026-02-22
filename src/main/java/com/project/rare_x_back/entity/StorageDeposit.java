package com.project.rare_x_back.entity;

import com.project.rare_x_back.common.FeeCalculator;
import com.project.rare_x_back.enums.StoragePaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@Table(name = "storage_deposits")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class StorageDeposit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "deposit_id")
    private Long depositId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storage_request_id", nullable = false)
    private StorageRequest storageRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder.Default
    @Column(name = "amount", nullable = false)
    private int amount = FeeCalculator.STORAGE_DEPOSIT;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StoragePaymentStatus status = StoragePaymentStatus.PENDING;

    @Column(name = "toss_payment_key")
    private String tossPaymentKey;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "cancel_idempotency_key")
    private String cancelIdempotencyKey;

    // === 비즈니스 메서드 ===

    public void markSuccess(String tossPaymentKey) {
        this.status = StoragePaymentStatus.SUCCESS;
        this.tossPaymentKey = tossPaymentKey;
        this.paidAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.status = StoragePaymentStatus.FAILED;
    }

    public void markCanceled() {
        this.status = StoragePaymentStatus.CANCELED;
    }

    public void assignCancelIdempotencyKey(String cancelIdempotencyKey) {
        this.cancelIdempotencyKey = cancelIdempotencyKey;
    }
}