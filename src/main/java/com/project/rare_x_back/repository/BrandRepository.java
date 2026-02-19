package com.project.rare_x_back.repository;

import com.project.rare_x_back.entity.Brand;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    List<Brand> findBrandsByIsDeletedFalse(Pageable pageable);

    boolean existsByBrandName(String brandName);
}
