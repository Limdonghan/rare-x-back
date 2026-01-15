package com.project.rare_x_back.controller;

import com.project.rare_x_back.common.ApiResponse;
import com.project.rare_x_back.dto.request.PasswordlessRequestDto;
import com.project.rare_x_back.dto.response.PasswordlessResponseDto;
import com.project.rare_x_back.service.PasswordlessService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/passwordless")
@RequiredArgsConstructor
public class PasswordlessController {

    private final PasswordlessService passwordlessService;

    // 단순 가입 확인
    @PostMapping("/verify-access")
    public ResponseEntity<ApiResponse<PasswordlessResponseDto>> verifyManagementAccess(
            @RequestParam String email,
            @RequestParam String password) {
        PasswordlessResponseDto response = passwordlessService.verifyManagementAccess(email, password);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 패스워드리스 등록여부 확인 (isAp)
    @GetMapping("/registration-status")
    public ResponseEntity<ApiResponse<PasswordlessResponseDto>> checkRegistrationStatus(
            @RequestParam String email) {
        PasswordlessResponseDto response = passwordlessService.checkRegistrationStatus(email);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 패스워드리스 신규 등록 (joinAp)
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<PasswordlessResponseDto>> registerPasswordless(
            @Valid @RequestBody PasswordlessRequestDto request,
            HttpServletRequest httpRequest) {
        PasswordlessResponseDto response = passwordlessService.registerPasswordless(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 패스워드리스 해지 (withdrawalAp)
    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<PasswordlessResponseDto>> withdrawPasswordless(
            @Valid @RequestBody PasswordlessRequestDto request) {
        PasswordlessResponseDto response = passwordlessService.withdrawPasswordless(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 일회용 토큰 요청
    @GetMapping("/one-time-token")
    public ResponseEntity<ApiResponse<PasswordlessResponseDto>> getOneTimeToken(
            @RequestParam String email) {
        PasswordlessResponseDto response = passwordlessService.getOneTimeToken(email);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 인증 요청 (getSp)
    @PostMapping("/authenticate")
    public ResponseEntity<ApiResponse<PasswordlessResponseDto>> requestAuthentication(
            @RequestParam String email,
            @RequestParam String token,
            HttpServletRequest httpRequest) {
        PasswordlessResponseDto response = passwordlessService.requestAuthentication(email, token, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 모바일 승인 여부 확인 (result)
    @GetMapping("/auth-result")
    public ResponseEntity<ApiResponse<PasswordlessResponseDto>> checkAuthenticationResult(
            @RequestParam String email,
            @RequestParam String sessionId) {
        PasswordlessResponseDto response = passwordlessService.checkAuthenticationResult(email, sessionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 인증 취소
    @PostMapping("/cancel")
    public ResponseEntity<ApiResponse<PasswordlessResponseDto>> cancelAuthentication(
            @RequestParam String email,
            @RequestParam String sessionId) {
        PasswordlessResponseDto response = passwordlessService.cancelAuthentication(email, sessionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
