package com.project.rare_x_back.controller;

import com.project.rare_x_back.common.CustomUserDetails;
import com.project.rare_x_back.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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

}