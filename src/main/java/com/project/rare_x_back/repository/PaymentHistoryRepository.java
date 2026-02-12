package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.PaymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, Long> {

    Optional<PaymentHistory> findByOrder_OrderId(Long orderId);
}
