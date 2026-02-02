package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    Optional<Settlement> findByOrder_OrderId(Long orderId);

    // 정산 정보 한 번에 조회 (N+1 방지)
    List<Settlement> findByOrder_OrderIdIn(List<Long> orderIds);
}
