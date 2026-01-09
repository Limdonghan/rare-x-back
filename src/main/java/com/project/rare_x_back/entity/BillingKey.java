package com.project.rare_x_back.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "billing_keys")
@EntityListeners(AuditingEntityListener.class)
public class BillingKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "billing_key_id")
    private Long billingKeyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "billing_key", nullable = false, unique = true)
    private String billingKey;      /// 토스에서 발급받은 자동 결제 키 (핵심)

    @Column(name = "card_company")
    private String cardCompany;     /// 카드사 정보 (예: 현대, 삼성)

    @Column(name = "card_number_last4")
    private String cardNumber;      /// 마스킹된 카드번호 (예: 4221-****-****-1234) 뒷자리 4자리

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

}
