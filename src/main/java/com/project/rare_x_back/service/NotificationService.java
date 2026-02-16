package com.project.rare_x_back.service;

import com.project.rare_x_back.entity.Notification;
import com.project.rare_x_back.entity.User;
import com.project.rare_x_back.enums.NotificationType;
import com.project.rare_x_back.repository.NotificationRepository;
import com.project.rare_x_back.repository.SseRepository;
import com.project.rare_x_back.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SseRepository sseRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /// 기본 타임아웃: 60분
    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60;

    /**
     * 클라이언트가 SSE 연결을 요청할 때 호출
     */
    public SseEmitter subscribe(Long userId) {
        /// 1. 현재 만료시간 설정하여 Emitter 생성
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

        /// 2. 생성된 Emitter를 저장 (유저 ID + 시간으로 고유 ID 생성 - Last-Event-ID 활용 가능)
        String emitterId = userId + "_" + System.currentTimeMillis();
        sseRepository.save(emitterId, emitter);

        /// 3. Emitter가 완료되거나 타임아웃 되면 저장소에서 제거
        emitter.onCompletion(() -> sseRepository.deleteById(emitterId));
        emitter.onTimeout(() -> sseRepository.deleteById(emitterId));
        emitter.onError((e) -> sseRepository.deleteById(emitterId));

        /// 4. 503 Service Unavailable 오류 방지를 위한 더미 이벤트 전송
        sendToClient(emitter, emitterId, emitterId, "EventStream Created. [userId=" + userId + "]");

        return emitter;
    }

    /**
     * 특정 유저에게 알림을 전송
     */
    public void send(Long userId, String content, String url, NotificationType type) {
        /// 1. 알림 엔티티 저장
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Notification notification = Notification.builder()
                .userId(user)
                .message(content)
                .url(url)
                .type(type)
                .isRead(false)
                .build();
        
        notificationRepository.save(notification);

        String eventId = userId + "_" + System.currentTimeMillis();

        /// 2. 유저의 모든 SseEmitter를 가져와서 알림 전송 (다중 기기 접속 고려)
        Map<String, SseEmitter> emitters = sseRepository.findAllEmitterStartWithByUserId(String.valueOf(userId));
        emitters.forEach(
                (emitterId, emitter) -> {
                    /// 데이터 캐시 저장 (유실 방지 - Last-Event-ID 사용 시 필요)
                    sseRepository.saveEventCache(emitterId, notification);
                    /// 데이터 전송
                    sendToClient(emitter, emitterId, eventId, notification); // emitterId 추가 전달
                }
        );
    }
    
    /// 알림 전송 공통 로직
    private void sendToClient(SseEmitter emitter, String emitterId, String eventId, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .id(eventId)
                    .name("notification")
                    .data(data));
        } catch (IOException e) {
            sseRepository.deleteById(emitterId); // 올바른 emitterId로 삭제
            log.error("SSE 연결 오류 [emitterId={}]: {}", emitterId, e.getMessage());
            // 예외를 던지지 않음 (다른 로직에 영향 주지 않기 위해)
        }
    }


}
