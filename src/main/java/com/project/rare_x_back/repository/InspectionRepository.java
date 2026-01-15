package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.Inspection;
import com.project.rare_x_back.enums.InspectionStatus;
import com.project.rare_x_back.enums.InspectionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InspectionRepository extends JpaRepository<Inspection, Long> {

    // 전체 목록 조회 (보관 검수용, Fetch Join)
    @Query("SELECT i FROM Inspection i " +
            "LEFT JOIN FETCH i.storageRequest sr " +    // 검수와 연결된 '보관 요청' 정보
            "LEFT JOIN FETCH sr.product p " +   // 보관 요청에 담긴 '상품' 정보
            "LEFT JOIN FETCH p.brand " +    // 그 상품의 '브랜드' 정보
            "LEFT JOIN FETCH sr.user " +    // 물건을 맡긴 '사용자' 정보
            "WHERE i.type = :type " +
            "ORDER BY i.createdAt ASC")     // 먼저 신청된 순서(오래된 순)
    List<Inspection> findByTypeWithDetails(@Param("type") InspectionType type);     // 특정 검수 유형(예: 보관 검수)에 해당하는 모든 목록

    // 상태별 목록 조회
    @Query("SELECT i FROM Inspection i " +
            "LEFT JOIN FETCH i.storageRequest sr " +
            "LEFT JOIN FETCH sr.product p " +
            "LEFT JOIN FETCH p.brand " +
            "LEFT JOIN FETCH sr.user " +
            "WHERE i.type = :type AND i.status = :status " +
            "ORDER BY i.createdAt ASC")
    List<Inspection> findByTypeAndStatusWithDetails(@Param("type") InspectionType type, // @Param은 쿼리문 안의 :type 자리에 메서드 파라미터로 들어온 type 변수 값을 집어넣어 주는 연결고리
                                                    @Param("status") InspectionStatus status);      // 특정 타입이면서 특정 상태(예: 검수 대기 중인 보관 검수)인 것만 필터링

    // 상태별 카운트
    long countByTypeAndStatus(InspectionType type, InspectionStatus status);    // 조건에 맞는 데이터가 **몇 개인지만 숫자(long)**로 반환 (ex : 현재 검수 대기 중인 물량 5건)
}