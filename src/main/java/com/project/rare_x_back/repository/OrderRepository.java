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

    @Query("""
        select o from Order o
        where o.currentStatus = 'DELIVERED'
        and o.updatedAt <= :time
        """)
    List<Order> findDeliveredOrders(@Param("time") LocalDateTime time);

    // 동시성 제어를 위한 비관적 락
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.orderId = :id")
    Optional<Order> findByIdWithLock(@Param("id") Long id);

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
    @EntityGraph(attributePaths = {"product", "product.images"})
    Page<Order> findBySeller_UserId(Long userId, Pageable pageable);

    // 판매 내역 상태 필터 조회
    @EntityGraph(attributePaths = {"product", "product.images"})
    Page<Order> findBySeller_UserIdAndCurrentStatusIn(Long userId, List<CurrentStatus> statuses, Pageable pageable);
}
