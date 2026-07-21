package com.acorn.abc.library.service;

import com.acorn.abc.external.library.client.InfoNaruLibraryClient;
import com.acorn.abc.external.library.dto.InfoNaruLibrarySearchResult;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

// 정보나루 outbound 동시 호출을 fair Semaphore(10, true)로 제한한다(계획 5.3-1, 5.5).
// permit을 즉시 얻지 못하면 빈 Optional을 반환하고, 호출측이 stale cache -> manual fallback -> 503 순서로 대체한다.
@Component
public class InfoNaruLibraryConcurrencyLimiter {

    private static final int MAX_CONCURRENT_CALLS = 10;
    private static final long ACQUIRE_TIMEOUT_MS = 200;

    private final Semaphore semaphore = new Semaphore(MAX_CONCURRENT_CALLS, true);
    private final InfoNaruLibraryClient client;

    public InfoNaruLibraryConcurrencyLimiter(InfoNaruLibraryClient client) {
        this.client = client;
    }

    public Optional<InfoNaruLibrarySearchResult> search(String isbn, String region, String dtlRegion,
                                                          int page, int size) {
        boolean acquired;
        try {
            acquired = semaphore.tryAcquire(ACQUIRE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
        if (!acquired) {
            return Optional.empty();
        }
        try {
            return Optional.of(client.search(isbn, region, dtlRegion, page, size));
        } finally {
            semaphore.release();
        }
    }
}
