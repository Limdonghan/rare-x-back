package com.project.rare_x_back.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.project.rare_x_back.common.ApiResponse;
import com.project.rare_x_back.dto.response.PasswordlessResponseDto;
import com.project.rare_x_back.service.PasswordlsessApiService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@AllArgsConstructor
@RequestMapping("/api/serving")
public class PasswordlessApiController {

    private final PasswordlsessApiService passwordlsessApiService;

    /// 패스워드리스 사용자 등록 확인
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Object>> checkUserStatus(@RequestParam("email") String email) {
        Boolean isEnabled = passwordlsessApiService.checkLocalUserStatus(email);
        Map<String, Boolean> data = new HashMap<>();
        data.put("exist", isEnabled);
        
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /// 패스워드리스 외부 등록 상태 확인 (Polling용)
    @GetMapping("/status/external")
    public ResponseEntity<ApiResponse<Object>> checkExternalUserStatus(@RequestParam("email") String email) {
        Boolean exists = passwordlsessApiService.checkUserStatus(email);
        Map<String, Boolean> data = new HashMap<>();
        data.put("exist", exists);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /// 패스워드리스 사용자 등록
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestParam("email") String email) {
        String registerUserResult = passwordlsessApiService.registerUser(email);
        return ResponseEntity.ok(registerUserResult);
    }

    /// 패스워드리스 활성화
    @PostMapping("/enable")
    public ResponseEntity<?> enableUser(@RequestParam("email") String email) {
        passwordlsessApiService.passwordlessEnabled(email);
        return ResponseEntity.ok("패스워드리스 활성화");
    }

    /// 패스워드리스 로그인 인증 요청
    @PostMapping("login-trigger")
    public ResponseEntity<?> loginTrigger(@RequestParam("email") String email,@RequestParam("ip") String ip) {
        String triggerLoginResult = passwordlsessApiService.triggerLogin(email, ip);
        return ResponseEntity.ok(triggerLoginResult);
    }

    /// 패스워드리스 인증 결과 확인
    @GetMapping("result")
    public ResponseEntity<PasswordlessResponseDto> result(@RequestParam("email") String email, @RequestParam("sessionId") String sessionId) throws JsonProcessingException {
        PasswordlessResponseDto passwordlessResponseDto = passwordlsessApiService.checkResult(email, sessionId);
        return ResponseEntity.ok(passwordlessResponseDto);
    }

    /// 인증 취소
    @PostMapping("/cancel")
    public ResponseEntity<Object> cancel(@RequestParam String email, @RequestParam String sessionId) {
        String cancel = passwordlsessApiService.cancel(email, sessionId);
        return ResponseEntity.ok(cancel);
    }

    /// 사용자 탈퇴
    @PostMapping("/withdrawal")
    public ResponseEntity<PasswordlessResponseDto> withdrawal(@RequestParam String email) {
        PasswordlessResponseDto withdrawalAp = passwordlsessApiService.withdrawalAp(email);
        return ResponseEntity.ok(withdrawalAp);
    }

}
