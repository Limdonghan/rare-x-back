package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    Optional<Settlement> findByOrder_OrderId(Long orderId);
}
