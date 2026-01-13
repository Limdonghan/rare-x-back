package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.StorageItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StorageItemRepository extends JpaRepository<StorageItem, Long> {

    // 사용자별 보관 상품 목록 조회
    List<StorageItem> findByUserUserId(Long userId);
}
