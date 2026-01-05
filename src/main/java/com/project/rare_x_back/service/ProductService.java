package com.project.rare_x_back.service;

import com.project.rare_x_back.dto.request.CategoryCreateRequestDto;
import com.project.rare_x_back.dto.request.ProductCreateRequestDto;
import com.project.rare_x_back.entity.Brand;
import com.project.rare_x_back.entity.Category;
import com.project.rare_x_back.entity.Product;
import com.project.rare_x_back.repository.BrandRepository;
import com.project.rare_x_back.repository.CategoryRepository;
import com.project.rare_x_back.repository.ProductRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;

    public void createProduct(ProductCreateRequestDto productCreateRequestDto) {

        Brand brand = brandRepository.findById(productCreateRequestDto.getBrandId())
                .orElseThrow(() -> new IllegalArgumentException("해당 브랜드 없음"));

        Category category = categoryRepository.findById(productCreateRequestDto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("해당 카테고리 없음"));

        Product product = Product.builder()
                .productName(productCreateRequestDto.getProductName())
                .productDescription(productCreateRequestDto.getProductDescription())
                .retailPrice(productCreateRequestDto.getRetailPrice())
                .brandEntity(brand)
                .categoryEntity(category)
                .build();

        productRepository.save(product);

    }
}
