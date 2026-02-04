package com.project.rare_x_back.controller;

import com.project.rare_x_back.common.ApiResponse;
import com.project.rare_x_back.common.CustomUserDetails;
import com.project.rare_x_back.dto.response.WishResponseDto;
import com.project.rare_x_back.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    // ===== WISH-002 관심 상품 등록 =====
    @PostMapping("/api/product/{productId}/wish")
    public ResponseEntity<ApiResponse<WishResponseDto>> addWish(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long productId) {

        WishResponseDto response = wishlistService.addWish(userDetails.getUserId(), productId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ===== WISH-002 관심 상품 해제 =====
    @DeleteMapping("/api/product/{productId}/wish")
    public ResponseEntity<ApiResponse<WishResponseDto>> removeWish(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long productId) {

        WishResponseDto response = wishlistService.removeWish(userDetails.getUserId(), productId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}