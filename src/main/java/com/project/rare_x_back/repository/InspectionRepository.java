package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.Inspection;
import com.project.rare_x_back.enums.InspectionStatus;
import com.project.rare_x_back.enums.InspectionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InspectionRepository extends JpaRepository<Inspection, Long> {

    // 주문 ID 목록으로 검수 정보 일괄 조회
    List<Inspection> findByOrder_OrderIdIn(List<Long> orderIds);

    // ===== VER-001 검수 대기 목록 조회 (페이징) =====

    // 전체 조회 (타입 무관)
    @EntityGraph(attributePaths = {
            "user",
            "storageRequest",
            "storageRequest.product",
            "storageRequest.product.brand",
            "storageRequest.user",
            "order",
            "order.product",
            "order.product.brand",
            "order.buyer",
            "order.seller"
    })
    @Query("SELECT i FROM Inspection i")
    Page<Inspection> findAllWithDetails(Pageable pageable);

    // 상태별 전체 조회 (타입 무관)
    @EntityGraph(attributePaths = {
            "user",
            "storageRequest",
            "storageRequest.product",
            "storageRequest.product.brand",
            "storageRequest.user",
            "order",
            "order.product",
            "order.product.brand",
            "order.buyer",
            "order.seller"
    })
    @Query("SELECT i FROM Inspection i WHERE i.status = :status")
    Page<Inspection> findByStatus(@Param("status") InspectionStatus status, Pageable pageable);

    // 타입별 전체 조회
    @EntityGraph(attributePaths = {
            "user",
            "storageRequest",
            "storageRequest.product",
            "storageRequest.product.brand",
            "storageRequest.user",
            "order",
            "order.product",
            "order.product.brand",
            "order.buyer",
            "order.seller"
    })
    @Query("SELECT i FROM Inspection i WHERE i.type = :type")
    Page<Inspection> findByType(@Param("type") InspectionType type, Pageable pageable);

    // 타입 + 상태별 조회
    @EntityGraph(attributePaths = {
            "user",
            "storageRequest",
            "storageRequest.product",
            "storageRequest.product.brand",
            "storageRequest.user",
            "order",
            "order.product",
            "order.product.brand",
            "order.buyer",
            "order.seller"
    })
    @Query("SELECT i FROM Inspection i WHERE i.type = :type AND i.status = :status")
    Page<Inspection> findByTypeAndStatus(@Param("type") InspectionType type, @Param("status") InspectionStatus status, Pageable pageable);

    // 타입 + 상태별 건수 조회 (대시보드용)
    long countByTypeAndStatus(InspectionType type, InspectionStatus status);


    // ===== VER-004 검수 이력 조회용 (페이징) =====

    // 검수 이력 조회 - 완료된 검수 전체 (PASSED + FAILED)
    @EntityGraph(attributePaths = {
            "user",
            "storageRequest",
            "storageRequest.product",
            "storageRequest.product.brand",
            "storageRequest.product.category",
            "storageRequest.user",
            "order",
            "order.product",
            "order.product.brand",
            "order.product.category",
            "order.buyer",
            "order.seller"
    })
    @Query("SELECT i FROM Inspection i WHERE i.status IN :statuses")
    Page<Inspection> findHistoryByStatusIn(@Param("statuses") List<InspectionStatus> statuses, Pageable pageable);

    // 검수 이력 조회 - 상태별 (PASSED만 또는 FAILED만)
    @EntityGraph(attributePaths = {
            "user",
            "storageRequest",
            "storageRequest.product",
            "storageRequest.product.brand",
            "storageRequest.product.category",
            "storageRequest.user",
            "order",
            "order.product",
            "order.product.brand",
            "order.product.category",
            "order.buyer",
            "order.seller"
    })
    @Query("SELECT i FROM Inspection i WHERE i.status = :status")
    Page<Inspection> findHistoryByStatus(@Param("status") InspectionStatus status, Pageable pageable);
}