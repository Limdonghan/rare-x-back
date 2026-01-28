package com.project.rare_x_back.controller;

import com.project.rare_x_back.common.ApiResponse;
import com.project.rare_x_back.common.CustomUserDetails;
import com.project.rare_x_back.dto.response.BuyingOrderDetailResponseDto;
import com.project.rare_x_back.dto.response.BuyingOrderResponseDto;
import com.project.rare_x_back.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mypage/orders")
public class OrderController {

    private final OrderService orderService;

    // 주문 내역 조회 (ORDER-002)
    @GetMapping("/buying")
    public ResponseEntity<ApiResponse<Page<BuyingOrderResponseDto>>> getBuyingOrders(
            @RequestParam(defaultValue = "ALL") String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Page<BuyingOrderResponseDto> result = orderService.getBuyingOrders(
                userDetails.getUserId(),
                status,
                pageable
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // 주문 상세 조회 (ORDER-003)
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<BuyingOrderDetailResponseDto>> getBuyingOrderDetail(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        BuyingOrderDetailResponseDto result = orderService.getBuyingOrderDetail(
                userDetails.getUserId(),
                orderId
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}