package com.project.rare_x_back.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "order_shipping_snapshot")
@EntityListeners(AuditingEntityListener.class)
public class OrderShippingSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long shippingSnapshotId;

    // 1:1 관계 설정 (주문 하나당 스냅샷 하나)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Address 엔티티를 참조하지 않고, 주문 시점의 정보를 String으로 직접 저장
    @Column(nullable = false, length = 50)
    private String recipientName;

    @Column(nullable = false, length = 10)
    private String postalCode;

    @Column(nullable = false, length = 200)
    private String address;

    @Column(length = 100)
    private String detailAddress;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public OrderShippingSnapshot(Order order, String recipientName, String postalCode, String address, String detailAddress) {
        this.order = order;
        this.recipientName = recipientName;
        this.postalCode = postalCode;
        this.address = address;
        this.detailAddress = detailAddress;
    }

    // 정적 팩토리 메서드 (Address 엔티티로부터 데이터를 복사해 생성)
    public static OrderShippingSnapshot from(Order order, Address address) {
        return OrderShippingSnapshot.builder()
                .order(order)
                .recipientName(address.getRecipientName())
                .postalCode(address.getPostalCode())
                .address(address.getAddress())
                .detailAddress(address.getDetailAddress())
                .build();
    }

}
