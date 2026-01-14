package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.StorageItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StorageItemRepository extends JpaRepository<StorageItem, Long> {

    // Fetch Join으로 Product, Brand 함께 조회
    @Query("SELECT s FROM StorageItem s " +
            "JOIN FETCH s.product p " +
            "JOIN FETCH p.brand " +
            "WHERE s.user.userId = :userId")
    List<StorageItem> findByUserUserId(@Param("userId") Long userId);
}
