package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTossPaymentKey(String tossPaymentKey);

    // 주문 ID로 결제 정보 조회
    Optional<Payment> findByOrder_OrderId(Long orderId);
}
