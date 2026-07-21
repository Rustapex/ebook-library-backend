package com.acorn.abc.library.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

// client IP별 fixed-window rate limit (분당 30회, 계획 5.5). window key에 분 단위 epoch를 포함해
// 창이 바뀌면 자연히 새 counter로 교체되고 Caffeine expireAfterWrite로 오래된 window는 자동 정리된다.
@Component
public class LibraryRateLimiter {

    private static final int LIMIT_PER_MINUTE = 30;

    private final Cache<String, AtomicInteger> counters = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(2))
            .maximumSize(100_000)
            .build();

    public boolean tryConsume(String clientIp) {
        long windowMinute = Instant.now().getEpochSecond() / 60;
        String key = clientIp + ":" + windowMinute;
        AtomicInteger counter = counters.get(key, k -> new AtomicInteger(0));
        return counter.incrementAndGet() <= LIMIT_PER_MINUTE;
    }
}
