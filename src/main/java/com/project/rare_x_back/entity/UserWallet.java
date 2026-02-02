package com.project.rare_x_back.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "user_wallets", uniqueConstraints = {@UniqueConstraint(columnNames = "user_id")})
public class UserWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id")
    private Long walletId;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "balance", nullable = false)
    private Long balance = 0L;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static UserWallet create(User user) {
        UserWallet wallet = new UserWallet();
        wallet.user = user;
        wallet.balance = 0L;
        return wallet;
    }

    public void increase(long settleAmount) {
        if (settleAmount <= 0L) {
            throw new IllegalArgumentException("정산 적립 금액은 0보다 커야 합니다.");
        }
        this.balance += settleAmount;
    }

}
