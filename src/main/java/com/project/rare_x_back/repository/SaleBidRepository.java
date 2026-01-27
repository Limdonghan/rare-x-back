package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.Product;
import com.project.rare_x_back.entity.SaleBid;
import com.project.rare_x_back.enums.BidStatus;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SaleBidRepository extends JpaRepository<SaleBid, Long> {

    /// [추가] 상품별 상태별 조회
    List<SaleBid> findAllByProductAndStatus(Product product, BidStatus status);

    /// [추가] 상품별 상태별 가격순 조회
    List<SaleBid> findByProductAndStatusOrderByPriceAsc(Product product, BidStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<SaleBid> findAllByProductAndPriceAndStatusOrderByCreatedAtAsc(Product productId, int price, BidStatus status);

    // 베스트 매칭
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT s
    FROM SaleBid s
    WHERE s.status = :status
      AND s.product = :product
      AND s.price <= :buyPrice
      AND s.user.userId <> :buyerId
    ORDER BY s.price ASC, s.createdAt ASC
  """)
    List<SaleBid> findMatchTargetForBuy(
            @Param("product") Product product,
            @Param("status") BidStatus status,
            @Param("buyPrice") int buyPrice,
            @Param("buyerId") Long buyerId,
            Pageable pageable
    );
}
