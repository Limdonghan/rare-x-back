package com.project.rare_x_back.controller;

import com.project.rare_x_back.common.ApiResponse;
import com.project.rare_x_back.common.CustomUserDetails;
import com.project.rare_x_back.dto.response.UserSettlementHistoryDto;
import com.project.rare_x_back.dto.response.UserWalletAccountResponseDto;
import com.project.rare_x_back.service.UserWalletService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
@RequestMapping("/api/mypage/settlement")
public class UserSettlementController {

    private final UserWalletService userWalletService;

    // 유저 지갑 조회
    @GetMapping("/wallet")
    public ResponseEntity<ApiResponse<UserWalletAccountResponseDto>> userWallet(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UserWalletAccountResponseDto dto = userWalletService.userWalletAccount(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    // 유저 지갑 상세 조회 (정산 내역)
    @GetMapping("/wallet/detailHistory")
    public ResponseEntity<ApiResponse<UserSettlementHistoryDto>> userWalletDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails
            ){
        UserSettlementHistoryDto dto = userWalletService.userSettlementHistory(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

}
