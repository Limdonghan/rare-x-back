package com.project.rare_x_back.controller;

import com.project.rare_x_back.common.ApiResponse;
import com.project.rare_x_back.common.CustomUserDetails;
import com.project.rare_x_back.dto.response.*;
import com.project.rare_x_back.service.BidService;
import com.project.rare_x_back.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mypage/orders")
public class UserOrderController {

    private final BidService bidService;
    private final OrderService orderService;

    // ========== 구매 관련 API ==========

    // 구매 내역 목록 조회 (ORDER-002)
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

    // 구매 상세 조회 (ORDER-003)
    @GetMapping("/buying/{orderId}")
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

    // 구매자 구매 확정 (ORDER-004)
    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<ApiResponse<Void>> purchaseConfirm(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long orderId
    ) {
        orderService.userConfirmPurchase(orderId, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("구매 확정이 완료 되었습니다."));
    }

    // ========== 판매 관련 API ==========

    // 판매자 검수센터로 발송 처리 (ORDER-006)
    @PostMapping("/{orderId}/ship")
    public ResponseEntity<ApiResponse<OrderShipResponseDto>> shipOrderToWarehouse(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long orderId) {

        OrderShipResponseDto response = bidService.shipOrderToWarehouse(userDetails.getUsername(), orderId);

        return ResponseEntity.ok(ApiResponse.success(response, "발송 처리가 완료되었습니다."));
    }

    // 판매 내역 목록 조회 (ORDER-006)
    @GetMapping("/sales")
    public ResponseEntity<ApiResponse<Page<SellingOrderResponseDto>>> getSellingOrders(
            @RequestParam(defaultValue = "ALL") String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Page<SellingOrderResponseDto> result = orderService.getSellingOrders(
                userDetails.getUserId(),
                status,
                pageable
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // 판매 상세 조회 (ORDER-007)
    @GetMapping("/sales/{orderId}")
    public ResponseEntity<ApiResponse<SellingOrderDetailResponseDto>> getSellingOrderDetail(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        SellingOrderDetailResponseDto result = orderService.getSellingOrderDetail(
                userDetails.getUserId(),
                orderId
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // 구매자 거래 취소
    @PostMapping("buying/{orderId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelOrderByBuyer (
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        orderService.cancelByBuyer(userDetails.getUserId(), orderId);
        return ResponseEntity.ok(ApiResponse.success("구매 취소 완료 및 패널티 발생"));
    }

    // 판매자 거래 취소
    @PostMapping("/sales/{orderId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelOrderBySeller (
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        orderService.cancelBySeller(userDetails.getUserId(), orderId);
        return ResponseEntity.ok(ApiResponse.success("판매 취소 완료 및 구매자 환불 완료"));
    }

}