package com.project.rare_x_back.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;


@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "product_description", nullable = false)
    private String productDescription;

    @Column(name = "retail_price", nullable = false)
    private int retailPrice;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = true)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at", nullable = true)
    private LocalDateTime deletedAt;

    @ManyToOne
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @Builder
    public Product(
            String productName,
            String productDescription,
            int retailPrice,
            Brand brand,
            Category category
    ) {
        this.productName = productName;
        this.productDescription = productDescription;
        this.retailPrice = retailPrice;
        this.brand = brand;
        this.category = category;
    }

    public void updateProductInfo(
            String productName,
            String productDescription,
            Integer retailPrice,
            Brand brand,
            Category category
    ) {
        if (productName != null) {
            this.productName = productName;
        }
        if (productDescription != null) {
            this.productDescription = productDescription;
        }
        if (retailPrice != null) {
            this.retailPrice = retailPrice;
        }
        if (brand != null) {
            this.brand = brand;
        }
        if (category != null) {
            this.category = category;
        }
    }

}
