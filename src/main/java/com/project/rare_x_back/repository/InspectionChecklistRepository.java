package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.InspectionChecklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InspectionChecklistRepository extends JpaRepository<InspectionChecklist, Long> {

    // 검수 ID로 체크리스트 조회
    @Query("SELECT ic FROM InspectionChecklist ic " +
            "JOIN FETCH ic.inspection " +
            "WHERE ic.inspection.inspectionId = :inspectionId")
    Optional<InspectionChecklist> findByInspectionId(@Param("inspectionId") Long inspectionId);
}