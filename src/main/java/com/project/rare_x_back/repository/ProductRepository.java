package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    @EntityGraph(attributePaths = {"brand", "category", "images"})
    Optional<Product> findByProductIdAndIsDeletedFalse(Long productId);

    @EntityGraph(attributePaths = {"images"})
    Page<Product> findAllByIsDeletedFalse(Pageable pageable);
}
