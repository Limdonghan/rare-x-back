package com.project.rare_x_back.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "brands")
public class Brand {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long brandId;

    @Column(name = "brand_name", nullable = false)
    private String brandName;

    @Column(name = "is_deleted", nullable = true)
    private boolean isDeleted;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = true)
    private LocalDateTime updatedAt;

    @Builder
    public Brand(
            String brandName
    ) {
        this.brandName = brandName;
    }

    public void updateBrandInfo(String brandName) {
        this.brandName=brandName;
    }
}
