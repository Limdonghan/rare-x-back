package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.Product;
import com.project.rare_x_back.entity.StorageItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StorageItemRepository extends JpaRepository<StorageItem, Long> {

    // Fetch Join을 사용하여 StorageItem을 가져올 때 Product와 Brand 정보를 한 번에 묶어서 조회
    // 이렇게 하면 나중에 데이터를 꺼내 쓸 때 DB에 다시 물어볼 필요가 없어 성능이 좋아짐
    @Query("SELECT s FROM StorageItem s " +
            "JOIN FETCH s.product p " +
            "JOIN FETCH p.brand " +
            "WHERE s.user.userId = :userId")
    List<StorageItem> findByUserUserId(@Param("userId") Long userId);


    StorageItem findByProduct(Product product);
}
