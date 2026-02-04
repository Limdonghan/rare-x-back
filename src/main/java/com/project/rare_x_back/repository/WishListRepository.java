package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.WishList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WishListRepository extends JpaRepository<WishList, WishList.WishListId> {

    // 등록 여부 확인 ("이미 찜했는지" TRUE/FALSE)
    boolean existsByUserUserIdAndProductProductId(Long userId, Long productId);

    // 삭제용 조회
    Optional<WishList> findByUserUserIdAndProductProductId(Long userId, Long productId);

    // ===== WISH-001 관심 상품 목록 조회 =====
    @EntityGraph(attributePaths = {
            "product",
            "product.brand",
            "product.images"
    })
    Page<WishList> findByUserUserId(Long userId, Pageable pageable);
}