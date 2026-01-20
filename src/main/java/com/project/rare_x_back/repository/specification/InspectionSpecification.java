package com.project.rare_x_back.repository.specification;

import com.project.rare_x_back.dto.request.InspectionSearchRequestDto;
import com.project.rare_x_back.entity.Inspection;
import com.project.rare_x_back.entity.Product;
import com.project.rare_x_back.entity.StorageRequest;
import com.project.rare_x_back.enums.InspectionStatus;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class InspectionSpecification {

    public static Specification<Inspection> searchInspectionHistory(InspectionSearchRequestDto condition) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. 완료된 검수만 조회 (PASSED 또는 FAILED)
            predicates.add(root.get("status").in(InspectionStatus.PASSED, InspectionStatus.FAILED));

            // 2. 기간 필터 (inspected_at 기준)
            if (condition.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("inspectedAt"), condition.getStartDate()));
            }
            if (condition.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("inspectedAt"), condition.getEndDate()));
            }

            // 3. 결과 필터 (PASSED / FAILED)
            if (condition.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), condition.getStatus()));
            }

            // 4. 검수자 필터
            if (condition.getInspectorId() != null) {
                predicates.add(cb.equal(root.get("user").get("userId"), condition.getInspectorId()));
            }

            // 5. 검수 타입 필터 (STORAGE / ORDER)
            if (condition.getType() != null) {
                predicates.add(cb.equal(root.get("type"), condition.getType()));
            }

            // 6. 카테고리 필터 (STORAGE 타입: storageRequest → product → category)
            if (condition.getCategoryId() != null) {
                Join<Inspection, StorageRequest> storageRequestJoin = root.join("storageRequest", JoinType.LEFT);
                Join<StorageRequest, Product> productJoin = storageRequestJoin.join("product", JoinType.LEFT);
                predicates.add(cb.equal(productJoin.get("category").get("categoryId"), condition.getCategoryId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}