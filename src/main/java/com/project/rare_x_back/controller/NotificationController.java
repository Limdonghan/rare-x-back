package com.project.rare_x_back.controller;

import com.project.rare_x_back.common.ApiResponse;
import com.project.rare_x_back.common.CustomUserDetails;
import com.project.rare_x_back.dto.response.NotificationResponseDto;
import com.project.rare_x_back.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 클라이언트의 이벤트 구독을 수락
     * MediaType.TEXT_EVENT_STREAM_VALUE = text/event-stream은 SSE를 위한 Mime Type 서버 -> 클라이언트로 이벤트 전송
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return notificationService.subscribe(userDetails.getUserId());
    }

    /**
     * 알림 목록 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponseDto>>> getNotifications(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<NotificationResponseDto> notificationResponseDtoList = notificationService.getNotification(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(notificationResponseDtoList, "알림 목록 조회가 완료되었습니다."));
    }

    /**
     * 알림 읽음 처리
     */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(ApiResponse.success(null, "알림 읽음 처리가 완료되었습니다."));
    }

    /**
     * 모든 알림 단일 일괄 읽음 처리
     */
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(@AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.markAllAsRead(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "모든 알림 일괄 읽음 처리가 완료되었습니다."));
    }

}