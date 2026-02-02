package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.Settlement;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE) // 조회할 때 락을 걸어버림
    @Query("select s from Settlement s where s.order.orderId = :orderId")
    Optional<Settlement> findByOrder_OrderId(Long orderId);
}
