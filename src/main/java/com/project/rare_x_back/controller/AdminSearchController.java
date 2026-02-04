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
import com.project.rare_x_back.service.OrderService;
import com.project.rare_x_back.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/search")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSearchController {

    private final SearchService searchService;
    private final OrderService orderService;
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
     * 관리자 주문 조회 (키워드 검색 + 필터)
     */
    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<Page<AdminOrderResponseDto>>> getAdminOrders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20) Pageable pageable) {

        // 날짜를 LocalDateTime으로 변환 (검색 범위 지정)
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(23, 59, 59, 999_999_999) : null;

        Page<AdminOrderResponseDto> result;

        // 키워드가 있으면 검색 서비스 사용
        if (keyword != null && !keyword.isBlank()) {
            result = searchService.searchOrders(keyword, status, startDateTime, endDateTime, pageable);
        } else {
            // 키워드가 없으면 일반 조회 서비스 사용
            result = orderService.getAdminOrders(status, startDateTime, endDateTime, pageable);
        }

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 관리자 주문 상세 조회
     */
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponse<AdminOrderDetailResponseDto>> getAdminOrderDetail(
            @PathVariable Long orderId) {
        AdminOrderDetailResponseDto result = orderService.getAdminOrderDetail(orderId);
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
        var products = productRepository.findAllForSync();
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
        List<Order> orders = orderRepository.findAllForSync();
        int count = searchService.syncAllOrders(orders);
        return ResponseEntity.ok(ApiResponse.success("주문 동기화 완료: " + count + "건"));
    }

    /**
     * 전체 검수 동기화
     */
    @PostMapping("/sync/inspections")
    public ResponseEntity<ApiResponse<String>> syncInspections() {
        List<Inspection> inspections = inspectionRepository.findAllForSync();
        int count = searchService.syncAllInspections(inspections);
        return ResponseEntity.ok(ApiResponse.success("검수 동기화 완료: " + count + "건"));
    }
}