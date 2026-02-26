package com.project.rare_x_back.service;

import com.project.rare_x_back.dto.response.NotificationResponseDto;
import com.project.rare_x_back.entity.Notification;
import com.project.rare_x_back.entity.User;
import com.project.rare_x_back.enums.EmailType;
import com.project.rare_x_back.enums.NotificationType;
import com.project.rare_x_back.exceptions.CustomException;
import com.project.rare_x_back.exceptions.ErrorCode;
import com.project.rare_x_back.repository.NotificationRepository;
import com.project.rare_x_back.repository.SseRepository;
import com.project.rare_x_back.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SseRepository sseRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

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

        /// 4. 503 Service Unavailable 오류 방지를 위한 더미 이벤트 전송 (JSON 포맷)
        sendToClient(emitter, emitterId, emitterId, Map.of("message", "EventStream Created. [userId=" + userId + "]"));

        return emitter;
    }

    /**
     * 특정 유저에게 알림을 전송
     */
    @Transactional
    // 알림 전송 (기본 EmailType used: NOTIFICATION)
    public void send(Long userId, String content, String url, NotificationType notificationType) {
        send(userId, content, url, notificationType, EmailType.NOTIFICATION);
    }

    // 알림 전송 (EmailType 지정 가능)
    @Transactional
    public void send(Long userId, String content, String url, NotificationType notificationType, EmailType emailType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        /// 1. 알림 저장
        Notification notification = Notification.builder()
                .user(user)
                .message(content)
                .url(url)
                .isRead(false)
                .type(notificationType)
                .build();
        Notification savedNotification = notificationRepository.save(notification);

        /// 2. SSE 전송
        Map<String, SseEmitter> emitters = sseRepository.findAllEmitterStartWithByUserId(String.valueOf(userId));

        NotificationResponseDto responseDto = NotificationResponseDto.from(savedNotification);

        emitters.forEach(
                (key, emitter) -> {
                    sseRepository.saveEventCache(key, responseDto);
                    String eventId = key + "_" + System.currentTimeMillis();
                    sendToClient(emitter, key, eventId, responseDto); // key (emitterId)를 사용
                }
        );

        /// 3. 이메일 전송 (비동기) - 지정된 EmailType 사용
        emailService.sendNotificationEmail(user.getEmail(), emailType, content);
    }
    
    /// 알림 전송 공통 로직
    private void sendToClient(SseEmitter emitter, String emitterId, String eventId, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .id(eventId)
                    .name("notification")
                    .data(data, MediaType.APPLICATION_JSON)); /// JSON 타입 명시
        } catch (IOException e) {
            sseRepository.deleteById(emitterId); // 올바른 emitterId로 삭제
            log.error("SSE 연결 오류 [emitterId={}]: {}", emitterId, e.getMessage());
            /// 예외를 던지지 않음 (다른 로직에 영향 주지 않기 위해)
        }
    }

    /**
     * 사용자의 모든 알림을 DTO리스트로 변환
     * */
    public List<NotificationResponseDto> getNotification(String userEmail){
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Notification> notificationList = notificationRepository.findAllByUser_UserIdOrderByCreatedAtDesc(user.getUserId());

        return notificationList.stream()
                        .map(NotificationResponseDto::from)
                .toList();
    }

    /**
     * 알림 읽음 처리
     * */
    public void markAsRead (Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

        notification.isReadUpdate(true);
        notificationRepository.save(notification);
    }

    /**
     * 특정 사용자 알림 모두 읽음 처리
     * 프론트엔드에서 n번의 단건 API 요청이 들어오는 대신,
     * 한 번의 요청으로 사용자의 모든 미확인 알림을 읽음 처리(isRead = true)
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    /**
     * 주기적으로(1분마다) Heartbeat 전송하여 503 에러 및 타임아웃 방지
     */
    @Scheduled(fixedRate = 60 * 1000)
    public void sendHeartbeat() {
        Map<String, SseEmitter> emitters = sseRepository.findAll();
        emitters.forEach((key, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .id(key)
                        .name("heartbeat")
                        .data(""));
            } catch (IOException e) {
                sseRepository.deleteById(key);
            }
        });
    }


}
