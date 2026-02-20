package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.BuyBid;
import com.project.rare_x_back.entity.Product;
import com.project.rare_x_back.enums.BidStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BuyBidRepository extends JpaRepository<BuyBid, Long> {

    List<BuyBid> findAllByProduct_ProductIdAndStatus(Long productId, BidStatus status);

    /// [추가] 상품별 상태별 가격순 조회
    List<BuyBid> findByProductAndStatusOrderByPriceAsc(Product product, BidStatus status);

    // 가격 수정, 삭제 중인 로우 잠김
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<BuyBid> findByBuyIdAndUser_UserId(Long buyId, Long userId);
  
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


    // WISH-001 상품별 최저가 배치 조회 : 여러 상품의 최저가를 한 번에 계산해서 가져오는 JPQL
    @Query("SELECT s.product.productId, MAX(s.price) FROM BuyBid s " +
            "WHERE s.product.productId IN :productIds AND s.status = :status " +
            "GROUP BY s.product.productId")
    List<Object[]> findHighestPriceByProductIds(@Param("productIds") List<Long> productIds,
                                               @Param("status") BidStatus status);


    // ===== 마이페이지 구매입찰 조회 (N+1 방지) =====

    @EntityGraph(attributePaths = {"product", "product.brand", "product.images"})
    @Query("SELECT b FROM BuyBid b WHERE b.user.userId = :userId ORDER BY b.createdAt DESC")
    List<BuyBid> findMyBuyBidsAll(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"product", "product.brand", "product.images"})
    @Query("SELECT b FROM BuyBid b WHERE b.user.userId = :userId AND b.status = :status ORDER BY b.createdAt DESC")
    List<BuyBid> findMyBuyBidsByStatus(@Param("userId") Long userId, @Param("status") BidStatus status);

    @EntityGraph(attributePaths = {"product", "product.brand", "product.images"})
    @Query("SELECT b FROM BuyBid b WHERE b.user.userId = :userId AND b.status IN :statuses ORDER BY b.createdAt DESC")
    List<BuyBid> findMyBuyBidsByStatuses(@Param("userId") Long userId, @Param("statuses") List<BidStatus> statuses);

    // ====== 관리자 회원 상세 - 활성 구매입찰 건수 (MANAGER-004) ======

    long countByUser_UserIdAndStatus(Long userId, BidStatus status);
}
