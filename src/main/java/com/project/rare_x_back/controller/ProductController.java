package com.project.rare_x_back.controller;

import com.project.rare_x_back.common.ApiResponse;
import com.project.rare_x_back.common.CustomUserDetails;
import com.project.rare_x_back.dto.response.*;
import com.project.rare_x_back.service.ProductService;
import com.project.rare_x_back.service.SearchService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/api/product")
public class ProductController {

    private final ProductService productService;
    private final SearchService searchService;

    //상품 전체 조회(목록, 썸네일이미지만 응답)
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponseDto>>> getAllPublicProd (
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<ProductResponseDto> response = productService.getAllPublicProd(categoryId, brandId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    //상품 상세 조회 (상품 전체 이미지 응답)
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductDetailResponseDto>> getPublicDetailProduct (
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = (userDetails != null) ? userDetails.getUserId() : null;
        ProductDetailResponseDto response = productService.getPublicDetailProduct(productId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    //상품 검색 (회원용)
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<SearchResultDto<ProductSearchResponseDto>>> searchProducts(
            @RequestParam String keyword,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        SearchResultDto<ProductSearchResponseDto> result = searchService.searchProducts(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // 전용 카테고리 목록 조회
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryListResponseDto>>> getAllCategories() {
        return ResponseEntity.ok(ApiResponse.success(productService.getAllCategories()));
    }

    // 전용 브랜드 목록 조회
    @GetMapping("/brands")
    public ResponseEntity<ApiResponse<List<BrandListResponseDto>>> getAllBrands() {
        return ResponseEntity.ok(ApiResponse.success(productService.getAllBrands()));
    }
}
