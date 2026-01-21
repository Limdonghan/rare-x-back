package com.project.rare_x_back.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "addresses")
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long addressId;

    @ManyToOne(fetch = FetchType.LAZY) // 지연 로딩 권장
    @JoinColumn(name = "user_id")   // DB에 member_id FK 컬럼이 생김
    private User user;

    @Column(name = "recipient_name", nullable = false)
    private String recipientName;

    @Column(name = "postal_code", nullable = false)
    private String postalCode;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "detail_address")
    private String detailAddress;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public Address(User user, String recipientName, String postalCode, String address, String detailAddress, boolean isDefault) {
        this.user = user;
        this.recipientName = recipientName;
        this.postalCode = postalCode;
        this.address = address;
        this.detailAddress = detailAddress;
        this.isDefault = isDefault;
    }

    // 주소 정보 상태 변경
    public void updateAddress(
            String recipientName,
            String detailAddress
    ) {
        if (recipientName != null && !recipientName.isBlank()) {
            this.recipientName = recipientName;
        }
        if (detailAddress != null && !detailAddress.isBlank()) {
            this.detailAddress = detailAddress;
        }
    }

    // 기본배송지 여부 상태 변경
    public void setAsDefault() {
        this.isDefault = true;
    }

    public void unsetDefault() {
        this.isDefault = false;
    }

}
