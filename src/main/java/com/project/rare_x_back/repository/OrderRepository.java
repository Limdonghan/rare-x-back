package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.Order;
import com.project.rare_x_back.enums.CurrentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // 구매 내역 전체 조회
    Page<Order> findByBuyer_UserId(Long userId, Pageable pageable);

    // 구매 내역 상태 필터 조회
    Page<Order> findByBuyer_UserIdAndCurrentStatusIn(Long userId, List<CurrentStatus> statuses, Pageable pageable);
}
