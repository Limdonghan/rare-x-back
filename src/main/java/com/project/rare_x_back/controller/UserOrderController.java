package com.project.rare_x_back.controller;

import com.project.rare_x_back.common.ApiResponse;
import com.project.rare_x_back.common.CustomUserDetails;
import com.project.rare_x_back.dto.response.OrderShipResponseDto;
import com.project.rare_x_back.service.BidService;
import com.project.rare_x_back.service.OrderService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/api/mypage/orders")
public class UserOrderController {

    private final BidService bidService;
    private final OrderService orderService;


    // 판매자 검수센터로 발송 처리
    @PostMapping("/{orderId}/ship")
    public ResponseEntity<ApiResponse<OrderShipResponseDto>> shipOrderToWarehouse(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long orderId) {

        OrderShipResponseDto response = bidService.shipOrderToWarehouse(userDetails.getUsername(), orderId);

        return ResponseEntity.ok(ApiResponse.success(response, "발송 처리가 완료되었습니다."));
    }

    // 구매자 구매 확정
    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<ApiResponse<Void>> purchaseConfirm (
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long orderId
    ) {
       orderService.userConfirmPurchase(orderId, userDetails.getUserId());
       return ResponseEntity.ok(ApiResponse.success("구매 확정이 완료되었습니다."));
    }




}
