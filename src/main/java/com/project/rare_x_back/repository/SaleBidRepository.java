package com.project.rare_x_back.repository;

import com.project.rare_x_back.dto.response.StorageProductResponseDto;
import com.project.rare_x_back.entity.Product;
import com.project.rare_x_back.entity.SaleBid;
import com.project.rare_x_back.enums.BidStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;

public interface SaleBidRepository extends JpaRepository<SaleBid, Long> {

    /// [추가] 상품별 상태별 조회
    List<SaleBid> findAllByProduct_ProductIdAndStatus(Long productId, BidStatus status);

    /// [추가] 상품별 상태별 가격순 조회
    List<SaleBid> findByProductAndStatusOrderByPriceAsc(Product product, BidStatus status);

    long countByProduct_ProductIdAndStatusAndStorageItemIsNotNull(
            Long productId,
            BidStatus status
    );

    // 즉시 구매용
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
      SELECT s
      FROM SaleBid s
      WHERE s.product = :product
        AND s.price = :price
        AND s.status = :status
        AND s.user.userId <> :buyerId
      ORDER BY s.createdAt ASC
    """)
    List<SaleBid> findAllByProductAndPriceAndStatusAndUserNot(
            @Param("product") Product product,
            @Param("price") int price,
            @Param("status") BidStatus status,
            @Param("buyerId") Long buyerId,
            Pageable pageable
    );



    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<SaleBid> findAllByProductAndPriceAndStatusOrderByCreatedAtAsc(Product productId, int price, BidStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SaleBid> findBySellIdAndUser_UserId(Long sellId, Long userId);
  
    // 베스트 매칭
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT s
    FROM SaleBid s
    LEFT JOIN FETCH s.storageItem
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


    // WISH-001 상품별 최저가 배치 조회 : 여러 상품의 최저가를 한 번에 계산해서 가져오는 JPQL
    @Query("SELECT s.product.productId, MIN(s.price) FROM SaleBid s " +
            "WHERE s.product.productId IN :productIds AND s.status = :status " +
            "GROUP BY s.product.productId")
    List<Object[]> findLowestPriceByProductIds(@Param("productIds") List<Long> productIds,
                                               @Param("status") BidStatus status);

    /// [추가] 보관 판매 상품 목록 조회 (상품별 그룹화, 최저가, 재고 수량)
    /// storageItem이 null이 아니고 status가 OPEN인 것들 대상
    @Query("""
            SELECT new com.project.rare_x_back.dto.response.StorageProductResponseDto(
                s.product.productId,
                s.product.brand.brandName,
                s.product.productName,
                MIN(s.price),
                COUNT(s)
            )
            FROM SaleBid s
            WHERE s.storageItem IS NOT NULL
            AND s.status = :status
            AND s.product.isDeleted = false
            GROUP BY s.product.productId, s.product.brand.brandName, s.product.productName
           """)
    List<StorageProductResponseDto> findStorageProducts(@Param("status") BidStatus status);

    // DB에 직접 UPDATE 쿼리 실행 (벌크 연산)
    @Modifying
    @Query("UPDATE SaleBid sb SET sb.status = :cancelStatus " +
            "WHERE sb.storageItem.storageId = :storageId " +
            "AND sb.status = :openStatus")
    void cancelByStorageId(
            @Param("storageId") Long storageId,
            @Param("cancelStatus") BidStatus cancelStatus,
            @Param("openStatus") BidStatus openStatus
    );

    // ===== 마이페이지 판매입찰 조회 (N+1 방지) =====

    @EntityGraph(attributePaths = {"product", "product.brand", "product.images"})
    @Query("SELECT s FROM SaleBid s WHERE s.user.userId = :userId")
    Page<SaleBid> findMySaleBidsAll(@Param("userId") Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"product", "product.brand", "product.images"})
    @Query("SELECT s FROM SaleBid s WHERE s.user.userId = :userId AND s.status = :status")
    Page<SaleBid> findMySaleBidsByStatus(@Param("userId") Long userId, @Param("status") BidStatus status, Pageable pageable);

    // ====== 관리자 회원 상세 - 활성 판매입찰 건수 (MANAGER-004) ======

    long countByUser_UserIdAndStatus(Long userId, BidStatus status);

    // 회원탈퇴 - OPEN 입찰 조회 (자동 취소용)
    List<SaleBid> findByUser_UserIdAndStatus(Long userId, BidStatus status);
}
