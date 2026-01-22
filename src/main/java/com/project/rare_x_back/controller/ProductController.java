package com.project.rare_x_back.controller;

import com.project.rare_x_back.common.ApiResponse;
import com.project.rare_x_back.dto.response.ProductDetailResponseDto;
import com.project.rare_x_back.dto.response.ProductResponseDto;
import com.project.rare_x_back.service.ProductService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/api/product")
public class ProductController {

    private final ProductService productService;

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
    public ResponseEntity<ApiResponse<ProductDetailResponseDto>> getPublicDetailProduct (@PathVariable Long productId) {
        ProductDetailResponseDto response = productService.getPublicDetailProduct(productId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }


}
