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

    // 상세 조회 (이미지까지 한방 로딩)
    @EntityGraph(attributePaths = {"buyer", "seller", "product", "product.images", "product.brand"})
    @Query("SELECT o FROM Order o WHERE o.orderId = :orderId")
    Optional<Order> findAdminOrderDetail(@Param("orderId") Long orderId);
}
