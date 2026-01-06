package com.project.rare_x_back.service;

import com.project.rare_x_back.dto.request.BrandCreateRequestDto;
import com.project.rare_x_back.dto.request.CategoryCreateRequestDto;
import com.project.rare_x_back.dto.request.ProductCreateRequestDto;
import com.project.rare_x_back.dto.request.ProductUpdateRequestDto;
import com.project.rare_x_back.entity.Brand;
import com.project.rare_x_back.entity.Category;
import com.project.rare_x_back.entity.Product;
import com.project.rare_x_back.repository.BrandRepository;
import com.project.rare_x_back.repository.CategoryRepository;
import com.project.rare_x_back.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AdminService {
    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;

    //상품 등록
    public void createProduct(ProductCreateRequestDto productCreateRequestDto) {

        Brand brand = brandRepository.findById(productCreateRequestDto.getBrandId())
                .orElseThrow(() -> new IllegalArgumentException("해당 브랜드 없음"));

        Category category = categoryRepository.findById(productCreateRequestDto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("해당 카테고리 없음"));

        Product product = Product.builder()
                .productName(productCreateRequestDto.getProductName())
                .productDescription(productCreateRequestDto.getProductDescription())
                .retailPrice(productCreateRequestDto.getRetailPrice())
                .brand(brand)
                .category(category)
                .build();

        productRepository.save(product);

    }

    //상품 정보 수정
    public void updateProduct(ProductUpdateRequestDto productUpdateRequestDto, Long productId){
        //수정할 상품 존재여부 확인
        Product product = productRepository.findById(productId)
                .orElseThrow(()-> new IllegalArgumentException("존재하지 않는 상품입니다.") );
        //수정된 값만 json채워 보내주고, 수정안된 값은 null로 채움
        Brand brand = null;
        if (productUpdateRequestDto.getBrandId() != null) {
            brand = brandRepository.findById(productUpdateRequestDto.getBrandId())
                    .orElseThrow(() -> new IllegalArgumentException("해당 브랜드 없음"));
        }
        Category category = null;
        if (productUpdateRequestDto.getCategoryId() != null) {
            category = categoryRepository.findById(productUpdateRequestDto.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("해당 카테고리 없음"));
        }
        //엔티티 업데이트
        product.updateProductInfo(productUpdateRequestDto, brand, category);
    }

    //카테고리 등록
    public void createCategory (CategoryCreateRequestDto categoryCreateRequestDto) {

        Category category = Category.builder()
                .categoryName(categoryCreateRequestDto.getCategoryName())
                .build();

        categoryRepository.save(category);
    }

    //브랜드 등록
    public void createBrand (BrandCreateRequestDto brandCreateRequestDto) {

        Brand brand = Brand.builder()
                .brandName(brandCreateRequestDto.getBrandName())
                .build();
        brandRepository.save(brand);
    }
}
