package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import com.project.rare_x_back.enums.CurrentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // 구매자에게 배송완료된 주문
    @Query("""
        select o from Order o
        where o.currentStatus = 'DELIVERED'
        and o.updatedAt <= :time
        """)
    List<Order> findDeliveredOrders(@Param("time") LocalDateTime time);

    // 발송 마감기한 지난 주문
    @Query("select o from Order o where o.currentStatus = 'PENDING' and o.sellerShippedAt IS NULL and o.expiresAt IS NULL and o.shipDeadline < :time")
    List<Order> findOrdersDeadLineBefore(@Param("time") LocalDateTime time);

    // 동시성 제어를 위한 비관적 락
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.orderId = :orderId")
    Optional<Order> findByIdWithLock(@Param("orderId") Long orderId);

    // 동기화용 - N+1 방지
    @EntityGraph(attributePaths = {
            "buyer",
            "seller",
            "product"
    })
    @Query("SELECT o FROM Order o")
    List<Order> findAllForSync();

    // 구매 내역 전체 조회
    @EntityGraph(attributePaths = {"product", "product.images"})
    Page<Order> findByBuyer_UserId(Long userId, Pageable pageable);

    // 구매 내역 상태 필터 조회
    @EntityGraph(attributePaths = {"product", "product.images"})
    Page<Order> findByBuyer_UserIdAndCurrentStatusIn(Long userId, List<CurrentStatus> statuses, Pageable pageable);

    // 판매 내역 전체 조회
    @EntityGraph(attributePaths = {"product", "product.images", "product.brand"})
    Page<Order> findBySeller_UserId(Long userId, Pageable pageable);

    // 판매 내역 상태 필터 조회
    @EntityGraph(attributePaths = {"product", "product.images", "product.brand"})
    Page<Order> findBySeller_UserIdAndCurrentStatusIn(Long userId, List<CurrentStatus> statuses, Pageable pageable);

    // ==========================================
    // [통계 그룹핑 전용] Inspection 존재 여부 체크 필터
    // ==========================================
    
    // 구매 내역 통계용 (Inspection 유무 체크 가능)
    @Query("""
        SELECT o FROM Order o
        LEFT JOIN Inspection i ON i.order = o
        WHERE o.buyer.userId = :userId
        AND o.currentStatus IN :statuses
        AND (:inspectionExists IS NULL 
             OR (:inspectionExists = true AND i IS NOT NULL) 
             OR (:inspectionExists = false AND i IS NULL))
        """)
    @EntityGraph(attributePaths = {"product", "product.images"})
    Page<Order> findByBuyerAndStatusWithInspectionCheck(
            @Param("userId") Long userId,
            @Param("statuses") List<CurrentStatus> statuses,
            @Param("inspectionExists") Boolean inspectionExists,
            Pageable pageable);

    // 판매 내역 통계용 (Inspection 유무 체크 가능)
    @Query("""
        SELECT o FROM Order o
        LEFT JOIN Inspection i ON i.order = o
        WHERE o.seller.userId = :userId
        AND o.currentStatus IN :statuses
        AND (:inspectionExists IS NULL 
             OR (:inspectionExists = true AND i IS NOT NULL) 
             OR (:inspectionExists = false AND i IS NULL))
        """)
    @EntityGraph(attributePaths = {"product", "product.images", "product.brand"})
    Page<Order> findBySellerAndStatusWithInspectionCheck(
            @Param("userId") Long userId,
            @Param("statuses") List<CurrentStatus> statuses,
            @Param("inspectionExists") Boolean inspectionExists,
            Pageable pageable);

    // ====== 관리자 주문 조회 (MANAGER-009) ======

    // 전체 조회
    @EntityGraph(attributePaths = {"buyer", "seller", "product"})
    @Query("SELECT o FROM Order o")
    Page<Order> findAllForAdmin(Pageable pageable);

    // 상태 필터
    @EntityGraph(attributePaths = {"buyer", "seller", "product"})
    Page<Order> findByCurrentStatusIn(List<CurrentStatus> statuses, Pageable pageable);

    // 날짜 필터
    @EntityGraph(attributePaths = {"buyer", "seller", "product"})
    Page<Order> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    // 상태 + 날짜 필터
    @EntityGraph(attributePaths = {"buyer", "seller", "product"})
    Page<Order> findByCurrentStatusInAndCreatedAtBetween(List<CurrentStatus> statuses, LocalDateTime start, LocalDateTime end, Pageable pageable);

    // 관리자 조회 통계용 (Inspection 유무 체크 가능)
    @Query("""
        SELECT o FROM Order o
        LEFT JOIN Inspection i ON i.order = o
        WHERE (o.currentStatus IN :statuses)
        AND (:startDate IS NULL OR o.createdAt >= :startDate)
        AND (:endDate IS NULL OR o.createdAt <= :endDate)
        AND (:inspectionExists IS NULL 
             OR (:inspectionExists = true AND i IS NOT NULL) 
             OR (:inspectionExists = false AND i IS NULL))
        """)
    @EntityGraph(attributePaths = {"buyer", "seller", "product"})
    Page<Order> findAllForAdminWithInspectionCheck(
            @Param("statuses") List<CurrentStatus> statuses,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("inspectionExists") Boolean inspectionExists,
            Pageable pageable);

    // 상세 조회 (이미지까지 한방 로딩)
    @EntityGraph(attributePaths = {"buyer", "seller", "product", "product.images", "product.brand"})
    @Query("SELECT o FROM Order o WHERE o.orderId = :orderId")
    Optional<Order> findAdminOrderDetail(@Param("orderId") Long orderId);

    // ====== 관리자 회원 상세 - 거래건수 (MANAGER-004) ======

    long countByBuyer_UserId(Long userId);

    long countBySeller_UserId(Long userId);


    // 랭킹 조회
    @Query(value = """
    SELECT 
        ranked.productId AS productId,
        p.product_name AS productName,
        pi.img_url AS thumbnailUrl,
        ranked.dealCount AS dealCount,
        b.name AS brandName,
        p.brand_id AS brandId,
        p.category_id AS categoryId,
        MIN(sb.price) AS lowestPrice
    FROM (
        SELECT 
            o.product_id AS productId,
            COUNT(o.order_id) AS dealCount
        FROM orders o
        WHERE o.current_status = 'CONFIRMED_PURCHASE'
          AND o.created_at >= :startDate
        GROUP BY o.product_id
        ORDER BY dealCount DESC
        LIMIT :limit
    ) ranked
    JOIN products p ON p.product_id = ranked.productId
    LEFT JOIN brands b ON b.brand_id = p.brand_id
    LEFT JOIN sell_bids sb ON sb.product_id = p.product_id AND sb.status = 'OPEN'
    LEFT JOIN (
        SELECT product_id, MIN(prod_img_id) AS min_image_id
        FROM product_images
        GROUP BY product_id
    ) pim ON pim.product_id = p.product_id
    LEFT JOIN product_images pi ON pi.prod_img_id = pim.min_image_id
    GROUP BY 
        ranked.productId, p.product_name, pi.img_url, 
        ranked.dealCount, b.name, p.brand_id, p.category_id
    """, nativeQuery = true)
    List<ProductRankingProjection> findRanking(
            @Param("startDate") LocalDateTime startDate,
            @Param("limit") int limit
    );

}
