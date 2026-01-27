package com.project.rare_x_back.controller;

import com.project.rare_x_back.common.ApiResponse;
import com.project.rare_x_back.dto.response.ProductSearchResponseDto;
import com.project.rare_x_back.dto.response.SearchResultDto;
import com.project.rare_x_back.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /**
     * 상품 검색 (회원용)
     * GET /api/search/products?keyword=나이키&page=0&size=20
     */
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<SearchResultDto<ProductSearchResponseDto>>> searchProducts(
            @RequestParam String keyword,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        SearchResultDto<ProductSearchResponseDto> result = searchService.searchProducts(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}