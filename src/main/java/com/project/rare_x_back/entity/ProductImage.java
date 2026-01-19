package com.project.rare_x_back.entity;

import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "product_images")
public class ProductImage {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long prodImgId;

    @Column(name = "img_url", nullable = false)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id") // DB의 외래키 컬럼명
    private Product product;

    @Builder
    public ProductImage(String imageUrl, Product product) {
        if (imageUrl == null) throw new CustomException(ErrorCode.BAD_REQUEST, "URL은 필수입니다.");
        this.imageUrl = imageUrl;
        this.product = product;
    }

}
