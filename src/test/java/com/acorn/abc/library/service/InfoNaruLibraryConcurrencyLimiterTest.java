package com.acorn.abc.library.service;

import com.acorn.abc.external.library.client.InfoNaruLibraryClient;
import com.acorn.abc.external.library.dto.InfoNaruLibrarySearchResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class InfoNaruLibraryConcurrencyLimiterTest {

    @Test
    void permit이_있으면_client_결과를_그대로_전달한다() {
        InfoNaruLibraryClient client = mock(InfoNaruLibraryClient.class);
        InfoNaruLibrarySearchResult expected = InfoNaruLibrarySearchResult.success(List.of(), 0);
        given(client.search("isbn", "11", null, 0, 20)).willReturn(expected);
        InfoNaruLibraryConcurrencyLimiter limiter = new InfoNaruLibraryConcurrencyLimiter(client);

        Optional<InfoNaruLibrarySearchResult> result = limiter.search("isbn", "11", null, 0, 20);

        assertThat(result).contains(expected);
    }

    @Test
    void 동시_호출은_최대_10개로_제한되고_permit을_얻지_못하면_빈_Optional을_반환한다() throws Exception {
        CountDownLatch releaseLatch = new CountDownLatch(1);
        CountDownLatch startedLatch = new CountDownLatch(10);
        AtomicInteger concurrentCalls = new AtomicInteger(0);
        AtomicInteger maxConcurrentCalls = new AtomicInteger(0);

        InfoNaruLibraryClient client = mock(InfoNaruLibraryClient.class);
        given(client.search("isbn", "11", null, 0, 20)).willAnswer(invocation -> {
            int current = concurrentCalls.incrementAndGet();
            maxConcurrentCalls.updateAndGet(max -> Math.max(max, current));
            startedLatch.countDown();
            releaseLatch.await(5, TimeUnit.SECONDS);
            concurrentCalls.decrementAndGet();
            return InfoNaruLibrarySearchResult.success(List.of(), 0);
        });

        InfoNaruLibraryConcurrencyLimiter limiter = new InfoNaruLibraryConcurrencyLimiter(client);
        ExecutorService pool = Executors.newFixedThreadPool(11);
        try {
            List<Future<Optional<InfoNaruLibrarySearchResult>>> occupying = new java.util.ArrayList<>();
            for (int i = 0; i < 10; i++) {
                occupying.add(pool.submit(() -> limiter.search("isbn", "11", null, 0, 20)));
            }
            assertThat(startedLatch.await(5, TimeUnit.SECONDS)).isTrue();

            Future<Optional<InfoNaruLibrarySearchResult>> eleventh =
                    pool.submit(() -> limiter.search("isbn", "11", null, 0, 20));
            assertThat(eleventh.get(5, TimeUnit.SECONDS)).isEmpty();

            releaseLatch.countDown();
            for (Future<Optional<InfoNaruLibrarySearchResult>> future : occupying) {
                assertThat(future.get(5, TimeUnit.SECONDS)).isPresent();
            }
            assertThat(maxConcurrentCalls.get()).isLessThanOrEqualTo(10);
        } finally {
            pool.shutdownNow();
        }
    }
}
