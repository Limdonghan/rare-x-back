package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    //특정 상품에 속한 모든 이미지 리스트를 찾고 싶을 때
    List<ProductImage> findByProdImgId(Long productId);

    //특정 상품의 이미지를 한꺼번에 삭제할 때
    //void deleteByProdImgId(Long productId);

}
