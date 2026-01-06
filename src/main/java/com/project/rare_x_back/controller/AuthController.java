package com.project.rare_x_back.controller;

import com.project.rare_x_back.common.ApiResponse;
import com.project.rare_x_back.dto.request.EmailVerifyRequest;
import com.project.rare_x_back.dto.request.LoginRequest;
import com.project.rare_x_back.dto.request.SignUpRequest;
import com.project.rare_x_back.dto.response.LoginResponse;
import com.project.rare_x_back.dto.response.SignUpResponse;
import com.project.rare_x_back.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignUpResponse>> signUp(@Valid @RequestBody SignUpRequest request) {
        SignUpResponse response = authService.signUp(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)  // 201 상태 코드
                .body(ApiResponse.success(response, "회원가입이 완료되었습니다"));
    }

    /// 이메일 인증 번호 발송 이거 없음
    /// 이메일 인증 번호 확인

    // 이메일인증
    @PostMapping("/email/verify")
    public ResponseEntity<ApiResponse<Void>> verifyEmailController(@Valid @RequestBody EmailVerifyRequest request) {

        authService.verifyEmail(request);

        return ResponseEntity
                .ok(ApiResponse.success("이메일 인증이 완료되었습니다"));
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);

        return ResponseEntity
                .ok(ApiResponse.success(response, "로그인 성공"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        // 추후 Redis 블랙리스트 추가
        // - 토큰을 Redis에 저장 (만료 시간까지)
        // - 매 요청마다 블랙리스트 확인
        return ResponseEntity.ok(ApiResponse.success("로그아웃 되었습니다"));
    }
}
