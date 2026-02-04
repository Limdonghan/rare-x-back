package com.project.rare_x_back.entity;

import com.project.rare_x_back.enums.PenaltyReason;
import com.project.rare_x_back.enums.PenaltyRole;
import com.project.rare_x_back.enums.PenaltyStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_penalties")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserPenalty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "penalty_id")
    private Long penaltyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PenaltyRole role;   // BUYER / SELLER

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PenaltyReason reason;

    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PenaltyStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime processedAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }



    public void markPaid() {
        this.status = PenaltyStatus.PAID;
        this.processedAt = LocalDateTime.now();
    }
}
