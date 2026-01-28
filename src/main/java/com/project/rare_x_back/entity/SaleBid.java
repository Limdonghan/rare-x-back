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
@Table(name = "sell_bids")
@EntityListeners(AuditingEntityListener.class)
public class SaleBid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sell_id")
    private Long sellId;

    @JoinColumn(name = "user_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;                     /// 판매자

    @JoinColumn(name = "product_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Product product;              /// 판매 상품

    @JoinColumn(name = "storage_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private StorageItem storageItem;          /// 보관 상품

    @Column(name = "price")
    private int price;                      /// 판매 가격

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private BidStatus status;               /// 판매 상태

    @Column(name = "created_at")
    @CreatedDate
    private LocalDateTime createdAt;       /// 판매 등록일

    @Column(name = "expired_at")
    private LocalDateTime expiresAt;       /// 판매 마감일

    public void statusUpdate(BidStatus status){
        this.status = status;
    }

    public void salePriceUpdate(int price) {
        this.price = price;
    }

}
