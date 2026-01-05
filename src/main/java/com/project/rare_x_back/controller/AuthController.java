package com.project.rare_x_back.controller;

import com.project.rare_x_back.common.ApiResponse;
import com.project.rare_x_back.dto.request.SignUpRequest;
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
        SignUpResponse response = authService.signUP(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)  // 201 상태 코드
                .body(ApiResponse.success(response, "회원가입이 완료되었습니다"));
    }
}
