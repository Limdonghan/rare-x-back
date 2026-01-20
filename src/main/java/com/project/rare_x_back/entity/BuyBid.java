package com.project.rare_x_back.entity;

import com.project.rare_x_back.enums.BidStatus;
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
@Table(name = "buy_bids")
@EntityListeners(AuditingEntityListener.class)
public class BuyBid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "buy_id")
    private Long buyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User userId;                    /// 구매자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product productId;              /// 구매 상품

    @Column(name = "price")
    private int price;                      /// 구매 희망 가격

    @Column(name = "status")
    private BidStatus status;               /// 입찰 상태

    @Column(name = "created_at")
    @CreatedDate
    private LocalDateTime createdAt;       /// 구매 등록일

    @Column(name = "expired_at")
    private LocalDateTime expiresAt;       /// 구매 마감일

    public void statusUpdate(BidStatus status){
        this.status = status;
    }



}
