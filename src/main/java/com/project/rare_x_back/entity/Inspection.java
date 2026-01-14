package com.project.rare_x_back.entity;

import com.project.rare_x_back.enums.InspectionStatus;
import com.project.rare_x_back.enums.InspectionType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "inspections")
public class Inspection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inspection_id")
    private Long inspectionId;

    // 주문 검수용 (NULL 가능)
    @Column(name = "order_id")
    private Long orderId;

    // 보관 검수용 (NULL 가능)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storage_request_id")
    private StorageRequest storageRequest;

    // 검수 담당자 (관리자)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private InspectionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InspectionStatus status;

    @Column(name = "fail_reason", length = 100)
    private String failReason;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "inspected_at")
    private LocalDateTime inspectedAt;

    @Builder
    public Inspection(Long orderId, StorageRequest storageRequest, User user,
                      InspectionType type, InspectionStatus status) {
        this.orderId = orderId;
        this.storageRequest = storageRequest;
        this.user = user;
        this.type = type;
        this.status = status;
    }

    // 상태 변경
    public void updateStatus(InspectionStatus status) {
        this.status = status;
        if (status == InspectionStatus.PASSED || status == InspectionStatus.FAILED) {
            this.inspectedAt = LocalDateTime.now();
        }
    }

    // 불합격 처리
    public void fail(String failReason) {
        this.status = InspectionStatus.FAILED;
        this.failReason = failReason;
        this.inspectedAt = LocalDateTime.now();
    }
}
