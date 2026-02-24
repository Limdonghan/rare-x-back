package com.project.rare_x_back.controller;

import com.project.rare_x_back.common.ApiResponse;
import com.project.rare_x_back.common.CustomUserDetails;
import com.project.rare_x_back.dto.request.*;
import com.project.rare_x_back.dto.response.LoginResponseDto;
import com.project.rare_x_back.dto.response.RefreshTokenResponseDto;
import com.project.rare_x_back.dto.response.SignUpResponseDto;
import com.project.rare_x_back.security.JwtTokenProvider;
import com.project.rare_x_back.service.AuthService;
import com.project.rare_x_back.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignUpResponseDto>> signUp(@Valid @RequestBody SignUpRequestDto request) {

        SignUpResponseDto response = authService.signUp(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)  // 201 상태 코드
                .body(ApiResponse.success(response, "회원가입이 완료되었습니다."));
    }

    // 이메일 인증 번호 발송
    @PostMapping("/email/send")
    public ResponseEntity<ApiResponse<Void>> sendVerificationCode(
            @Valid @RequestBody EmailSendRequestDto request) {

        authService.sendVerificationCode(request);

        return ResponseEntity
                .ok(ApiResponse.success("인증번호가 발송되었습니다. 이메일을 확인해주세요."));
    }


    // 이메일 인증번호 확인
    @PostMapping("/email/verify")
    public ResponseEntity<ApiResponse<Void>> verifyEmailController(@Valid @RequestBody EmailVerifyRequestDto request) {

        authService.verifyEmail(request);

        return ResponseEntity
                .ok(ApiResponse.success("이메일 인증이 완료되었습니다"));
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@Valid @RequestBody LoginRequestDto request) {

        LoginResponseDto response = authService.login(request);

        return ResponseEntity
                .ok(ApiResponse.success(response, "로그인 성공"));
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestHeader("Authorization") String authHeader) {

        // "Bearer " 제거
        String accessToken = jwtTokenProvider.resolveToken(authHeader);

        authService.logout(userDetails.getUsername(), accessToken);
        return ResponseEntity.ok(ApiResponse.success("로그아웃 되었습니다"));
    }

    // Access Token 갱신
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponseDto>> refreshToken(
            @Valid @RequestBody RefreshTokenRequestDto request) {

        RefreshTokenResponseDto response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(response, "토큰이 갱신되었습니다"));
    }


    // 임시 비밀번호 발급 요청
    @PostMapping("/users/resetpw")
    public ResponseEntity<ApiResponse> requestPasswordReset(
            @RequestBody @Valid PasswordResetRequestDto request) {

        emailService.sendTempPassword(request.getEmail());

        return ResponseEntity.ok(
                ApiResponse.success("임시 비밀번호가 이메일로 발송되었습니다.")
        );
    }

    // 비밀번호 변경
    @PatchMapping("/users/changepw")
    public ResponseEntity<ApiResponse<?>> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PasswordChangeRequestDto dto
    ) {
        authService.changePassword(userDetails.getUserId(), dto);
        return ResponseEntity.ok(ApiResponse.success("비밀번호가 변경되었습니다."));
    }

}
