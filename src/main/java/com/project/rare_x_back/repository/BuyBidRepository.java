package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.BuyBid;
import com.project.rare_x_back.enums.BidStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuyBidRepository extends JpaRepository<BuyBid, Long> {

    // 사용자별 전체 입찰 목록 조회
    @EntityGraph(attributePaths = {"product", "product.brand"})
    Page<BuyBid> findByUserEmail(String email, Pageable pageable);

    // 사용자별 + 상태별 입찰 목록 조회
    @EntityGraph(attributePaths = {"product", "product.brand"})
    Page<BuyBid> findByUserEmailAndStatus(String email, BidStatus status, Pageable pageable);
}
