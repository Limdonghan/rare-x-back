package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.Settlement;
import com.project.rare_x_back.enums.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    @Query("select s from Settlement s where s.order.orderId = :orderId")
    Optional<Settlement> findByOrder_OrderId(Long orderId);

    // 유저 아이디와 상태로 조회
    List<Settlement> findAllBySeller_UserIdAndStatus(Long userId, SettlementStatus status);

    List<Settlement>findAllBySeller_UserIdAndStatusOrderByCompletedAtDesc(Long userId, SettlementStatus status);

}
