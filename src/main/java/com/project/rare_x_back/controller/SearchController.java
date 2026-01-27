package com.project.rare_x_back.controller;

import com.project.rare_x_back.common.ApiResponse;
import com.project.rare_x_back.dto.response.ProductSearchResultDto;
import com.project.rare_x_back.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final ProductSearchService productSearchService;

    /**
     * 상품 검색 (회원용)
     * GET /api/search/products?keyword=나이키&page=0&size=20
     */
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<ProductSearchResultDto>> searchProducts(
            @RequestParam String keyword,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        ProductSearchResultDto result = productSearchService.search(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}