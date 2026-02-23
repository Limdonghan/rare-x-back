package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.Order;
import com.project.rare_x_back.entity.OrderHistory;
import com.project.rare_x_back.enums.CurrentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface OrderHistoryRepository extends JpaRepository<OrderHistory, Long> {

    // 주문 ID로 상태 이력 조회 (시간순 정렬)
    List<OrderHistory> findByOrder_OrderIdOrderByCreatedAtAsc(Long orderId);


    Optional<OrderHistory> findTopByOrderAndCurrentStatusOrderByCreatedAtDesc(
            Order order, CurrentStatus status
    );
}
