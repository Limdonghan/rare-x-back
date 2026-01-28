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
@ToString(exclude = {"user","product"})
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
    private User user;                    /// 구매자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;              /// 구매 상품
  
    @Column(name = "address_id", nullable = false)
    private Long addressId;

//    @JoinColumn ( name = "address_id" , insertable = false , updatable = false )
//    @ManyToOne(fetch = FetchType.LAZY)
//    private Address address;

    @Column(name = "price")
    private int price;                      /// 구매 희망 가격

    @Enumerated(EnumType.STRING)
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

    public void buyPriceUpdate(int price) {
        this.price = price;
    }
    



}
