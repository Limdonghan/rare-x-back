package com.project.rare_x_back.service;

import com.project.rare_x_back.dto.response.ProductListResponseDto;
import com.project.rare_x_back.entity.Product;
import com.project.rare_x_back.entity.ProductImage;
import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import com.project.rare_x_back.repository.BrandRepository;
import com.project.rare_x_back.repository.CategoryRepository;
import com.project.rare_x_back.repository.ProductImageRepository;
import com.project.rare_x_back.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;

    public Page<ProductListResponseDto> getAllPublicProd(Long categoryId, Long brandId, Pageable pageable) {

        Page<Product> productPage;

        // 카테고리 + 브랜드 필터 적용 하는 경우
        if (categoryId != null && brandId != null) {
            productPage = productRepository.findByCategory_CategoryIdAndBrand_BrandIdAndIsDeletedFalse(categoryId, brandId, pageable);
        } else if (categoryId != null) { // 카테고리만 필터링
            productPage = productRepository.findByCategory_CategoryIdAndIsDeletedFalse(categoryId, pageable);
        } else if (brandId != null) { // 브랜드만 필터링
            productPage = productRepository.findByBrand_BrandIdAndIsDeletedFalse(brandId, pageable);
        } else {
                productPage = productRepository.findAllByIsDeletedFalse(pageable);
        }
        // DTO 변환 및 이미지 처리
        return productPage.map(product -> {
            // 이미지 리스트에서 첫 번째 이미지(썸네일) URL 추출
            String imageUrl = null;
            if (product.getImages() != null && !product.getImages().isEmpty()) {
                imageUrl = product.getImages().get(0).getImageUrl();
            }

            return ProductListResponseDto.builder()
                    .productId(product.getProductId())
                    .productName(product.getProductName())
                    .brandName(product.getBrand() != null ? product.getBrand().getBrandName() : " ")
                    .categoryName(product.getCategory() != null ? product.getCategory().getCategoryName() : " ")
                    .imageUrl(imageUrl) // 추출한 S3 URL 주입 (썸네일)
                    .build();
        });
    }

    public ProductListResponseDto getPublicDetailProduct(Long productId) {
        Product product = productRepository.findByProductIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "상품을 찾을 수 없습니다."
                ));

        // 이미지 객체 리스트를 URL만 있는 문자열 리스트로 변환
        List<String> imageUrls = product.getImages().stream()
                .map(ProductImage:: getImageUrl)
                .toList();

        return ProductListResponseDto.builder()
                .productId(productId)
                .productName(product.getProductName())
                .productDescription(product.getProductDescription())
                .brandName(product.getBrand() != null ? product.getBrand().getBrandName() : " ")
                .categoryName(product.getCategory() != null ? product.getCategory().getCategoryName() : " ")
                .imageUrls(imageUrls)
                .build();

    }




}
