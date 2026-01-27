package com.project.rare_x_back.controller;

import com.project.rare_x_back.common.ApiResponse;
import com.project.rare_x_back.dto.response.*;
import com.project.rare_x_back.entity.Inspection;
import com.project.rare_x_back.entity.Order;
import com.project.rare_x_back.entity.User;
import com.project.rare_x_back.repository.InspectionRepository;
import com.project.rare_x_back.repository.OrderRepository;
import com.project.rare_x_back.repository.ProductRepository;
import com.project.rare_x_back.repository.UserRepository;
import com.project.rare_x_back.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/search")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSearchController {

    private final SearchService searchService;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final InspectionRepository inspectionRepository;

    // ==================== 검색 API ====================

    /**
     * 상품 검색 (관리자용)
     */
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<SearchResultDto<ProductSearchResponseDto>>> searchProducts(
            @RequestParam String keyword,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        SearchResultDto<ProductSearchResponseDto> result = searchService.searchProducts(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 회원 검색
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<SearchResultDto<UserSearchResponseDto>>> searchUsers(
            @RequestParam String keyword,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        SearchResultDto<UserSearchResponseDto> result = searchService.searchUsers(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 주문 검색
     */
    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<SearchResultDto<OrderSearchResponseDto>>> searchOrders(
            @RequestParam String keyword,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        SearchResultDto<OrderSearchResponseDto> result = searchService.searchOrders(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 검수 검색
     */
    @GetMapping("/inspections")
    public ResponseEntity<ApiResponse<SearchResultDto<InspectionSearchResponseDto>>> searchInspections(
            @RequestParam String keyword,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        SearchResultDto<InspectionSearchResponseDto> result = searchService.searchInspections(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ==================== 초기 동기화 API ====================

    /**
     * 전체 상품 동기화
     */
    @PostMapping("/sync/products")
    public ResponseEntity<ApiResponse<String>> syncProducts() {
        var products = productRepository.findAllByIsDeletedFalse();
        int count = searchService.syncAllProducts(products);
        return ResponseEntity.ok(ApiResponse.success("상품 동기화 완료: " + count + "건"));
    }

    /**
     * 전체 회원 동기화
     */
    @PostMapping("/sync/users")
    public ResponseEntity<ApiResponse<String>> syncUsers() {
        List<User> users = userRepository.findAllByIsDeletedFalse();
        int count = searchService.syncAllUsers(users);
        return ResponseEntity.ok(ApiResponse.success("회원 동기화 완료: " + count + "건"));
    }

    /**
     * 전체 주문 동기화
     */
    @PostMapping("/sync/orders")
    public ResponseEntity<ApiResponse<String>> syncOrders() {
        List<Order> orders = orderRepository.findAll();
        int count = searchService.syncAllOrders(orders);
        return ResponseEntity.ok(ApiResponse.success("주문 동기화 완료: " + count + "건"));
    }

    /**
     * 전체 검수 동기화
     */
    @PostMapping("/sync/inspections")
    public ResponseEntity<ApiResponse<String>> syncInspections() {
        List<Inspection> inspections = inspectionRepository.findAll();
        int count = searchService.syncAllInspections(inspections);
        return ResponseEntity.ok(ApiResponse.success("검수 동기화 완료: " + count + "건"));
    }
}