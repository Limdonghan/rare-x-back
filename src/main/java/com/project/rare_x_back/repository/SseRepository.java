package com.project.rare_x_back.repository;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.Map;

public interface SseRepository {
    SseEmitter save(String emitterId, SseEmitter sseEmitter);
    void saveEventCache(String eventCacheId, Object event);
    Map<String, SseEmitter> findAllEmitterStartWithByUserId(String userId);
    Map<String, Object> findAllEventCacheStartWithByUserId(String userId);
    void deleteById(String emitterId);
    void deleteAllEmitterStartWithId(String userId);
    void deleteAllEventCacheStartWithId(String userId);
    Map<String, SseEmitter> findAll();
}
