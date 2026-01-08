package com.project.rare_x_back.service;

import com.project.rare_x_back.dto.request.*;
import com.project.rare_x_back.entity.Brand;
import com.project.rare_x_back.entity.Category;
import com.project.rare_x_back.entity.Product;
import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import com.project.rare_x_back.repository.BrandRepository;
import com.project.rare_x_back.repository.CategoryRepository;
import com.project.rare_x_back.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
                .orElseThrow(() -> new CustomException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "존재하지 않는 브랜드 입니다."
                        ));

        Category category = categoryRepository.findById(productCreateRequestDto.getCategoryId())
                .orElseThrow(() -> new CustomException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "존재하지 않는 카테고리 입니다."
                        ));

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
        Product product = productRepository.findByProductIdAndIsDeletedFalse(productId)
                .orElseThrow(()->
                        new CustomException(
                                ErrorCode.RESOURCE_NOT_FOUND,
                                "상품을 찾을 수 없습니다."
                        ));
        //수정된 값만 json채워 보내주고, 수정안된 값은 null로 채움
        Brand brand = null;
        if (productUpdateRequestDto.getBrandId() != null) {
            brand = brandRepository.findById(productUpdateRequestDto.getBrandId())
                    .orElseThrow(() -> new CustomException(
                            ErrorCode.RESOURCE_NOT_FOUND,
                            "존재하지 않는 브랜드입니다."
                    ));
        }

        Category category = null;
        if (productUpdateRequestDto.getCategoryId() != null) {
            category = categoryRepository.findById(productUpdateRequestDto.getCategoryId())
                    .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND,
                            "존재하지 않는 카테고리 입니다."
                    ));
        }
        //엔티티 업데이트
        product.updateProductInfo(
                productUpdateRequestDto.getProductName(),
                productUpdateRequestDto.getProductDescription(),
                productUpdateRequestDto.getRetailPrice(),
                brand,
                category
        );
    }

    //상품 삭제
    public void deleteProduct (Long productId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.RESOURCE_NOT_FOUND,
                                "상품을 찾을 수 없습니다."
                        )
                );

        productRepository.deleteById(product.getProductId());

    }

    //카테고리 등록
    public void createCategory (CategoryCreateRequestDto categoryCreateRequestDto) {

        Category category = Category.builder()
                .categoryName(categoryCreateRequestDto.getCategoryName())
                .build();

        categoryRepository.save(category);
    }

    //카테고리 수정
    public void updateCategory (CategoryUpdateRequestDto categoryUpdateRequestDto, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "카테고리를 찾을 수 없습니다."
                ));
        category.updateCategoryInfo(categoryUpdateRequestDto.getCategoryName());
    }

    //카테고리 삭제
    public void deleteCategory (Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(()->
                        new CustomException(
                                ErrorCode.RESOURCE_NOT_FOUND,
                                "카테고리를 찾을 수 없습니다.")
                );
        categoryRepository.deleteById(category.getCategoryId());
    }

    //브랜드 등록
    public void createBrand (BrandCreateRequestDto brandCreateRequestDto) {

        Brand brand = Brand.builder()
                .brandName(brandCreateRequestDto.getBrandName())
                .build();
        brandRepository.save(brand);
    }

    //브랜드 수정
    public void updateBrand (BrandUpdateRequestDto brandUpdateRequestDto, Long brandId) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "브랜드를 찾을 수 없습니다."
                ));
        brand.updateBrandInfo(brandUpdateRequestDto.getBrandName());
    }
}
