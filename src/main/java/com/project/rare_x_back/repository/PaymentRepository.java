package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTossPaymentKey(String tossPaymentKey);

    // 주문 ID로 결제 정보 조회
    Optional<Payment> findTopByOrder_OrderIdOrderByApprovedAtDesc(Long orderId);

    Optional<Payment> findByOrder_OrderId(Long orderId);


    // 취소 시 반드시 이 메서드로 조회
    // payment row에 PESSIMISTIC_WRITE 락
    //동시에 두 번 취소 못 하게 막음
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.tossPaymentKey = :paymentKey")
    Optional<Payment> findByTossPaymentKeyForUpdate(
            @Param("paymentKey") String paymentKey
    );
}
