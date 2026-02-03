package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.Settlement;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    @Query("select s from Settlement s where s.order.orderId = :orderId")
    Optional<Settlement> findByOrder_OrderId(Long orderId);

    // 정산 정보 한 번에 조회 (N+1 방지)
    @EntityGraph(attributePaths = {"order"})
    List<Settlement> findByOrder_OrderIdIn(List<Long> orderIds);
}
