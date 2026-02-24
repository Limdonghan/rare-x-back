package com.project.rare_x_back.controller;

import com.project.rare_x_back.common.ApiResponse;
import com.project.rare_x_back.common.CustomUserDetails;
import com.project.rare_x_back.dto.request.WithdrawRequestDto;
import com.project.rare_x_back.security.JwtTokenProvider;
import com.project.rare_x_back.service.WithdrawService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mypage/profile")
public class UserProfileController {

    private final WithdrawService withdrawService;
    private final JwtTokenProvider jwtTokenProvider;

    // 회원 탈퇴
    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody WithdrawRequestDto request,
            @RequestHeader("Authorization") String authHeader
    ) {
        String accessToken = jwtTokenProvider.resolveToken(authHeader);
        withdrawService.withdraw(userDetails.getUserId(), request.getPassword(), accessToken);
        return ResponseEntity.ok(ApiResponse.success("회원 탈퇴가 완료되었습니다."));
    }
}