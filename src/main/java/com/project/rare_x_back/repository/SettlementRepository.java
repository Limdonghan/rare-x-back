package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    @Query("select s from Settlement s where s.order.orderId = :orderId")
    Optional<Settlement> findByOrder_OrderId(Long orderId);
}
