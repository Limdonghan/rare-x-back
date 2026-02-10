package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.OrderShippingSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderShippingSnapshotRepository extends JpaRepository<OrderShippingSnapshot, Long> {

    // 주문 ID로 배송지 스냅샷 조회
    Optional<OrderShippingSnapshot> findByOrder_OrderId(Long orderId);
}
