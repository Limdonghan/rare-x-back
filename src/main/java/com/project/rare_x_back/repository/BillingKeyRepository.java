package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.BillingKey;
import com.project.rare_x_back.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BillingKeyRepository extends JpaRepository<BillingKey, Long> {

    /// 유저 ID로 빌링키 찾기


    Optional<BillingKey> findByUserId(User user);

    boolean existsByBillingKey(String billingKey);
}
