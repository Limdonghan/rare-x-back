package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.BuyBid;
import com.project.rare_x_back.entity.Product;
import com.project.rare_x_back.enums.BidStatus;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BuyBidRepository extends JpaRepository<BuyBid, Long> {

    /// [추가] 상품별 상태별 입찰 목록 조회
    List<BuyBid> findAllByProductAndStatus(Product product, BidStatus status);

    /// [추가] 상품별 상태별 가격순 조회
    List<BuyBid> findByProductAndStatusOrderByPriceAsc(Product product, BidStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT b
    FROM BuyBid b
    WHERE b.status = :status
      AND b.product = :product
      AND b.price >= :sellPrice
      AND b.user.userId <> :sellerId
    ORDER BY b.price DESC, b.createdAt ASC
  """)
    List<BuyBid> findMatchTargetForSale(
            @Param("product") Product product,
            @Param("status") BidStatus status,
            @Param("sellPrice") int sellPrice,
            @Param("sellerId") Long sellerId,
            Pageable pageable
    );
}
