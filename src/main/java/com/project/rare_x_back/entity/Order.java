package com.project.rare_x_back.entity;

import com.project.rare_x_back.enums.BidType;
import com.project.rare_x_back.enums.CurrentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "orders")
@EntityListeners(AuditingEntityListener.class)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id")
    private User buyer;                         /// 구매자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private User seller;                        /// 판매자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;                    /// 상품

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buy_bid_id")
    private BuyBid buyBid;                      /// 체결된 판매 입찰

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sell_bid_id")
    private SaleBid sellBid;                    /// 체결된 구매 입찰

    @Enumerated(EnumType.STRING)
    @Column(name = "bid_type")
    private BidType type;                       /// 입찰 구분

    @Column(name = "price")
    private int price;                          /// 거래 가격

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status")
    private CurrentStatus currentStatus;            /// 주문 상태

    @Column(name = "created_at")
    @CreatedDate
    private LocalDateTime createdAt;       /// 주문 생성일

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "expired_at")
    private LocalDateTime expiresAt;       /// 거래 만료일

    @Column(name = "seller_shipped_at")
    private LocalDateTime sellerShippedAt;

    @Column(name="ship_deadline")
    private LocalDateTime shipDeadline;


    // 상태 업데이트
    public void updateStatus(CurrentStatus newStatus) {
        this.currentStatus = newStatus;
    }






}
