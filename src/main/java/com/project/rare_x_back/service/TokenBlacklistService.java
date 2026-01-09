package com.project.rare_x_back.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String BLACKLIST_PREFIX = "blacklist:";


    //  Access Token 블랙리스트에 추가
    public void addToBlacklist(String accessToken, long expirationMillis) {
        if (expirationMillis > 0) {
            String key = BLACKLIST_PREFIX + accessToken;
            redisTemplate.opsForValue().set(key, "logout", expirationMillis, TimeUnit.MILLISECONDS);
        }
    }

    // 블랙리스트 확인
    public boolean isBlacklisted(String accessToken) {
        String key = BLACKLIST_PREFIX + accessToken;
        return redisTemplate.hasKey(key);
    }
}