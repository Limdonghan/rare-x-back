package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.Product;
import com.project.rare_x_back.entity.StorageItem;
import com.project.rare_x_back.enums.StorageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StorageItemRepository extends JpaRepository<StorageItem, Long> {

    // Fetch Join을 사용하여 StorageItem을 가져올 때 Product와 Brand, Images 정보를 한 번에 묶어서 조회
    @Query("SELECT s FROM StorageItem s " +
            "JOIN FETCH s.product p " +
            "JOIN FETCH p.brand " +
            "LEFT JOIN FETCH p.images " +
            "WHERE s.user.userId = :userId " +
            "ORDER BY s.storedAt DESC")
    List<StorageItem> findByUserUserId(@Param("userId") Long userId);


    StorageItem findByProduct(Product product);

    Optional<StorageItem> findFirstByUser_UserIdAndProduct_ProductIdAndStatusOrderByExpiredAtAsc(
            Long userId,
            Long productId,
            StorageStatus status
    );

    // SaleBid와 연결된 StorageItem 조회 (서브쿼리 대신 JOIN 사용하여 성능 최적화)
    @Query("SELECT si FROM StorageItem si JOIN SaleBid sb ON sb.storageItem.storageId = si.storageId WHERE sb.sellId = :sellId")
    Optional<StorageItem> findBySellBidId(@Param("sellId") Long sellId);

    /**
     * 180일 만료 + 활성 상태인 보관함 조회 (자동결제 대상)
     * user 함께 로딩 (N+1 방지)
     */
    @Query("SELECT s FROM StorageItem s " +
            "JOIN FETCH s.user " +
            "WHERE s.expiredAt < :now " +
            "AND s.status IN :statuses")
    List<StorageItem> findExpiredByStatuses(
            @Param("now") LocalDateTime now,
            @Param("statuses") List<StorageStatus> statuses
    );

    // 회원탈퇴 - 보관 중 상품 존재 여부
    boolean existsByUser_UserIdAndStatusIn(Long userId, List<StorageStatus> statuses);
}
