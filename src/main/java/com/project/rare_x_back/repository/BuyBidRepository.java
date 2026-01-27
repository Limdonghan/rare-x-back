package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.BuyBid;
import com.project.rare_x_back.entity.Product;
import com.project.rare_x_back.enums.BidStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BuyBidRepository extends JpaRepository<BuyBid, Long> {

    /// [추가] 상품별 상태별 입찰 목록 조회
    List<BuyBid> findAllByProductAndStatus(Product product, BidStatus status);

    /// [추가] 상품별 상태별 가격순 조회
    List<BuyBid> findByProductAndStatusOrderByPriceAsc(Product product, BidStatus status);

    // 유저아이디로 구매입찰 내역 조회
    List<BuyBid> findAllByUser_UserIdOrderByCreatedAtDesc(Long userId);
    List<BuyBid> findAllByUser_UserIdAndStatusOrderByCreatedAtDesc(Long userId, BidStatus status);
    Optional<BuyBid> findByBuyIdAndUser_UserId(Long buyId, Long userId);
}
