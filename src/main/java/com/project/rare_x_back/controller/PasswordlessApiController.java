package com.project.rare_x_back.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.project.rare_x_back.dto.response.PasswordlessResponseDto;
import com.project.rare_x_back.service.PasswordlsessApiService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api/serving")
public class PasswordlessApiController {

    private final PasswordlsessApiService passwordlsessApiService;

    /// 패스워드리스 사용자 등록 확인
    @GetMapping("/status")
    public ResponseEntity<?> checkUserStatus(@RequestParam("email") String email) {
        String status = passwordlsessApiService.checkUserStatus(email);
        return ResponseEntity.ok(status);
    }

    /// 패스워드리스 사용자 등록
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestParam("email") String email) {
        String registerUserResult = passwordlsessApiService.registerUser(email);
        return ResponseEntity.ok(registerUserResult);
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
