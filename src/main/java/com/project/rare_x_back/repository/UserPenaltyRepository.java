package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.UserPenalty;
import com.project.rare_x_back.enums.PenaltyRole;
import com.project.rare_x_back.enums.PenaltyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserPenaltyRepository extends JpaRepository<UserPenalty, Long> {

    // 특정 주문의 패널티 존재 여부
    boolean existsByOrder_OrderIdAndRole(
            Long order_orderId, PenaltyRole role
    );

    // 유저의 미납 패널티 조회
    List<UserPenalty> findByUser_UserIdAndStatus(
            Long userId,
            PenaltyStatus status
    );

    // 특정 주문 + 역할 패널티 조회
    Optional<UserPenalty> findByOrder_OrderIdAndRole(
            Long orderId,
            PenaltyRole role
    );

    // 회원탈퇴 - 미납 패널티 존재 여부
    boolean existsByUser_UserIdAndStatus(Long userId, PenaltyStatus status);
}