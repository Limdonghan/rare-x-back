package com.project.rare_x_back.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "wish_lists")
@IdClass(WishList.WishListId.class) // "두 개 이상의 필드를 묶어서 하나의 PK(기본키)로 사용하겠다"는 선언
public class WishList {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public WishList(Product product, User user) {
        this.product = product;
        this.user = user;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode  // "객체 안의 데이터(값)들이 전부 같으면 동일한 객체로 판별해라"는 명령
    public static class WishListId implements Serializable {    // Serializable : 직렬화가 가능하다
        private Long product;
        private Long user;
    }
}