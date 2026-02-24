package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = {"brand", "category", "images"})
    Optional<Product> findByProductIdAndIsDeletedFalse(Long productId);

    @EntityGraph(attributePaths = {"category", "brand","images"})
    Page<Product> findAllByIsDeletedFalse(Pageable pageable);

    //카테고리 필터링
    @EntityGraph(attributePaths = {"category", "brand", "images"})
    Page<Product> findByCategory_CategoryIdAndIsDeletedFalse(Long categoryId, Pageable pageable);

    //브랜드 필터링
    @EntityGraph(attributePaths = {"category", "brand", "images"})
    Page<Product> findByBrand_BrandIdAndIsDeletedFalse(Long brandId, Pageable pageable);

    // 카테고리 + 브랜드 필터링
    @EntityGraph(attributePaths = {"category", "brand", "images"})
    Page<Product> findByCategory_CategoryIdAndBrand_BrandIdAndIsDeletedFalse(Long categoryId, Long brandId, Pageable pageable);

    // 카테고리 다중 필터링
    @EntityGraph(attributePaths = {"category", "brand", "images"})
    Page<Product> findByCategory_CategoryIdInAndIsDeletedFalse(List<Long> categoryIds, Pageable pageable);

    // 브랜드 다중 필터링
    @EntityGraph(attributePaths = {"category", "brand", "images"})
    Page<Product> findByBrand_BrandIdInAndIsDeletedFalse(List<Long> brandIds, Pageable pageable);

    // 카테고리 + 브랜드 다중 필터링
    @EntityGraph(attributePaths = {"category", "brand", "images"})
    Page<Product> findByCategory_CategoryIdInAndBrand_BrandIdInAndIsDeletedFalse(List<Long> categoryIds, List<Long> brandIds, Pageable pageable);

    // 동기화용 - N+1 방지
    @EntityGraph(attributePaths = {"brand", "category"})
    @Query("SELECT p FROM Product p WHERE p.isDeleted = false")
    List<Product> findAllForSync();

    // N+1 문제 해결: 이미지 정보를 함께 조회 (Eager Fetching)
    @EntityGraph(attributePaths = {"images"})
    List<Product> findByProductIdIn(List<Long> productIds);


    /// 특정 카테고리에 속하며 삭제되지 않은 상품의 개수를 조회
    long countByCategory_CategoryIdAndIsDeletedFalse(Long categoryId);

    /// 특정 브랜드에 속하며 삭제되지 않은 상품의 개수를 조회
    long countByBrand_BrandIdAndIsDeletedFalse(Long brandId);

    ///  관심 상품 등록 시 wish_count +1 (Atomic UPDATE)
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Product p SET p.wishCount = p.wishCount + 1 WHERE p.productId = :productId")
    void incrementWishCount(@Param("productId") Long productId);

    ///  관심 상품 해제 시 wish_count -1 (Atomic UPDATE, 음수 방지)
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Product p SET p.wishCount = p.wishCount - 1 WHERE p.productId = :productId AND p.wishCount > 0")
    void decrementWishCount(@Param("productId") Long productId);

    // 카테고리 ID 목록 중 상품이 연결된 카테고리 ID 조회
    @Query("SELECT p.category.categoryId FROM Product p " +
            "WHERE p.category.categoryId IN :categoryIds AND p.isDeleted = false " +
            "GROUP BY p.category.categoryId")
    List<Long> findCategoryIdsWithProducts(@Param("categoryIds") List<Long> categoryIds);

    // 브랜드 ID 목록 중 상품이 연결된 브랜드 ID 조회
    @Query("SELECT p.brand.brandId FROM Product p " +
            "WHERE p.brand.brandId IN :brandIds AND p.isDeleted = false " +
            "GROUP BY p.brand.brandId")
    List<Long> findBrandIdsWithProducts(@Param("brandIds") List<Long> brandIds);
}
