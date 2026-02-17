package com.project.rare_x_back.repository;

import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class SseRepositoryImpl implements SseRepository {
    // thread-safe한 map을 사용하여 동시성을 보장 (Multi-thread 환경)
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final Map<String, Object> eventCache = new ConcurrentHashMap<>();

    /**
     * Emitter 저장
     * @param emitterId - {userId}_{timestamp} 형태로 생성된 고유 ID
     * @param sseEmitter - 생성된 SseEmitter 객체
     * @return 저장된 SseEmitter
     */
    @Override
    public SseEmitter save(String emitterId, SseEmitter sseEmitter) {
        emitters.put(emitterId, sseEmitter);
        return sseEmitter;
    }

    /**
     * 이벤트를 캐시에 저장 (유실된 데이터 전송을 위함)
     * @param eventCacheId - {userId}_{timestamp} 형태의 이벤트 ID
     * @param event - 전송된 데이터 객체 (Notification 등)
     */
    @Override
    public void saveEventCache(String eventCacheId, Object event) {
        eventCache.put(eventCacheId, event);
    }

    /**
     * 특정 회원과 관련된 모든 Emitter를 찾음
     * @param userId - 회원의 고유 ID (PK)
     * @return 해당 회원 ID로 시작하는 모든 Emitter 맵
     */
    @Override
    public Map<String, SseEmitter> findAllEmitterStartWithByUserId(String userId) {
        return emitters.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(userId))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * 특정 회원과 관련된 모든 이벤트 캐시를 찾음
     * @param userId - 회원의 고유 ID (PK)
     * @return 해당 회원 ID로 시작하는 모든 이벤트 맵
     */
    @Override
    public Map<String, Object> findAllEventCacheStartWithByUserId(String userId) {
        return eventCache.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(userId))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Emitter를 저장소에서 제거 (연결 종료, 타임아웃, 에러 발생 시)
     * @param emitterId - 제거할 Emitter의 고유 ID
     */
    @Override
    public void deleteById(String emitterId) {
        emitters.remove(emitterId);
    }

    /**
     * 특정 회원의 모든 Emitter 제거
     * @param userId - 제거할 회원의 ID
     */
    @Override
    public void deleteAllEmitterStartWithId(String userId) {
        // [수정] forEach 내에서 remove 호출 시 ConcurrentModificationException 발생 가능성 있음
        // keySet().removeIf()를 사용하여 안전하게 삭제
        emitters.keySet().removeIf(key -> key.startsWith(userId));
    }

    /**
     * 특정 회원의 모든 이벤트 캐시 제거
     * @param userId - 제거할 회원의 ID
     */
    @Override
    public void deleteAllEventCacheStartWithId(String userId) {
        // [수정] forEach 내에서 remove 호출 시 ConcurrentModificationException 발생 가능성 있음
        // keySet().removeIf()를 사용하여 안전하게 삭제
        eventCache.keySet().removeIf(key -> key.startsWith(userId));
    }
}
