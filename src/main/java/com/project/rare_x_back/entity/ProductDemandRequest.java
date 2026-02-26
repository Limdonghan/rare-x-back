package com.project.rare_x_back.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_demand_request")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductDemandRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long demandId;

    // 요청 유저
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 상품명
    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    // 브랜드명
    @Column(name = "brand_name", nullable = false, length = 255)
    private String brandName;

    // 발매가
    @Column(nullable = false, name = "retail_price")
    private Integer retailPrice;

    // 상품 설명 및 요청 사유
    @Column(nullable = false, name = "description", length = 500)
    private String description;

    // 이미지 URL
    @Column(nullable = false, length = 500, name = "image_url")
    private String imageUrl;

    // 생성일
    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private LocalDateTime createdAt;
}
