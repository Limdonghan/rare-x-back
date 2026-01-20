package com.project.rare_x_back.entity;

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
@Table(name = "inspection_checklists")
public class InspectionChecklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "checklist_id")
    private Long checklistId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspection_id", nullable = false)
    private Inspection inspection;

    @Column(name = "is_authentic", nullable = false)
    private Boolean isAuthentic = false;    // 정품 여부

    @Column(name = "is_exterior_good", nullable = false)
    private Boolean isExteriorGood = false;     // 외관 상태 양호 여부

    @Column(name = "is_components_complete", nullable = false)
    private Boolean isComponentsComplete = false;   // 구성품 완비 여부

    @Column(name = "is_packaging_good", nullable = false)
    private Boolean isPackagingGood = false;    // 포장 상태 양호 여부

    @Column(name = "is_unused", nullable = false)
    private Boolean isUnused = false;   // 미사용 여부

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;


    @Builder
    public InspectionChecklist(Inspection inspection) {
        this.inspection = inspection;
        this.isAuthentic = false;
        this.isExteriorGood = false;
        this.isComponentsComplete = false;
        this.isPackagingGood = false;
        this.isUnused = false;
    }

    // 체크리스트 항목 업데이트
    public void updateChecklist(Boolean isAuthentic, Boolean isExteriorGood,
                                Boolean isComponentsComplete, Boolean isPackagingGood,
                                Boolean isUnused) {
        this.isAuthentic = isAuthentic;
        this.isExteriorGood = isExteriorGood;
        this.isComponentsComplete = isComponentsComplete;
        this.isPackagingGood = isPackagingGood;
        this.isUnused = isUnused;
    }

    // 모든 항목 통과 여부 확인
    public boolean isAllPassed() {
        return isAuthentic && isExteriorGood && isComponentsComplete
                && isPackagingGood && isUnused;
    }
}