package com.project.rare_x_back.controller;

import com.project.rare_x_back.common.ApiResponse;
import com.project.rare_x_back.common.CustomUserDetails;
import com.project.rare_x_back.dto.response.BuyBidResponseDto;
import com.project.rare_x_back.enums.BidStatus;
import com.project.rare_x_back.service.BuyBidService;
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

    private final BuyBidService buyBidService;

    // 구매 입찰 목록 조회
    @GetMapping("/buy-bids")
    public ResponseEntity<ApiResponse<Page<BuyBidResponseDto>>> getBuyBids(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) BidStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<BuyBidResponseDto> response = buyBidService.getBuyBids(userDetails.getUsername(), status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 구매 입찰 취소
    @PatchMapping("/buy-bids/{buyId}/cancel")
    public ResponseEntity<ApiResponse<BuyBidResponseDto>> cancelBuyBid(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long buyId) {

        BuyBidResponseDto response = buyBidService.cancelBuyBid(userDetails.getUsername(), buyId);
        return ResponseEntity.ok(ApiResponse.success(response, "입찰이 취소되었습니다."));
    }
}