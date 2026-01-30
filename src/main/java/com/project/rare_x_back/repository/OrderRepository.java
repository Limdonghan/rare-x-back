package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("""
        select o from Order o
        where o.currentStatus = 'DELIVERED'
        and o.updatedAt <= :time
        """)
    List<Order> findDeliveredOrder(LocalDateTime time);

    @Query("""
    select o from Order o
    where o.currentStatus = 'CONFIRM_PURCHASE'
""")
    Optional<Order> findByCurrentStatus_ConfirmedPurchase(Long orderId);
}
