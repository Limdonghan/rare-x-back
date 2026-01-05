package com.project.rare_x_back.service;

import com.project.rare_x_back.dto.ProductCreateRequestDto;
import com.project.rare_x_back.entity.BrandEntity;
import com.project.rare_x_back.entity.CategoryEntity;
import com.project.rare_x_back.entity.ProductEntity;
import com.project.rare_x_back.repository.BrandRepository;
import com.project.rare_x_back.repository.CategoryRepository;
import com.project.rare_x_back.repository.ProductRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class AdminService {
    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;

    public long createProduct (ProductCreateRequestDto productCreateRequestDto) {

        BrandEntity brand = brandRepository.findById(productCreateRequestDto.getBrandId())
                .orElseThrow(() -> new IllegalArgumentException("해당 브랜드 없음"));

        CategoryEntity category = categoryRepository.findById(productCreateRequestDto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("해당 카테고리 없음"));

        ProductEntity product = ProductEntity.builder()
                .productName(productCreateRequestDto.getProductName())
                .productDescription(productCreateRequestDto.getProductDescription())
                .retailPrice(productCreateRequestDto.getRetailPrice())
                .brandEntity(brand)
                .categoryEntity(category)
                .build();

        ProductEntity saved = productRepository.save(product);

        return saved.getProductId();
    }

    }
