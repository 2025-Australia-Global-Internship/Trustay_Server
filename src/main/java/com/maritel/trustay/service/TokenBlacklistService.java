package com.maritel.trustay.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class TokenBlacklistService {

    // Key: 토큰 값, Value: 만료 시간 (Epoch Time)
    // 동시성 이슈를 방지하기 위해 ConcurrentHashMap 사용
    private final Map<String, Long> blacklistMap = new ConcurrentHashMap<>();

    /**
     * 토큰을 블랙리스트에 등록
     * @param token 블랙리스트에 추가할 토큰
     * @param expirationTime 토큰의 만료 시간 (이 시간이 지나면 메모리에서 삭제됨)
     */
    public void blacklistToken(String token, long expirationTime) {
        blacklistMap.put(token, expirationTime);
        log.info(">>>> [Logout] Token Blacklisted. Current Size: {}", blacklistMap.size());
    }

    /**
     * 토큰이 블랙리스트에 있는지 확인
     */
    public boolean isBlacklisted(String token) {
        return blacklistMap.containsKey(token);
    }

    /**
     * [스케줄러] 1분마다 만료된 토큰 삭제 (메모리 누수 방지)
     * 메인 클래스에 @EnableScheduling이 있어야 동작함
     */
    @Scheduled(fixedRate = 60000)
    public void cleanupExpiredTokens() {
        long now = System.currentTimeMillis();
        int beforeSize = blacklistMap.size();

        // 만료 시간이 현재 시간보다 이전인(이미 지난) 토큰 삭제
        blacklistMap.entrySet().removeIf(entry -> entry.getValue() < now);

        if (beforeSize != blacklistMap.size()) {
            log.debug(">>>> [Cleanup] Expired tokens removed. Size: {} -> {}", beforeSize, blacklistMap.size());
        }
    }
}