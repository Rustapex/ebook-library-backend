package com.acorn.abc.library.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LibraryRateLimiterTest {

    @Test
    void 분당_30회까지는_허용하고_31번째는_거부한다() {
        LibraryRateLimiter limiter = new LibraryRateLimiter();
        String ip = "203.0.113.10";

        for (int i = 0; i < 30; i++) {
            assertThat(limiter.tryConsume(ip)).isTrue();
        }
        assertThat(limiter.tryConsume(ip)).isFalse();
    }

    @Test
    void 서로_다른_IP는_독립적으로_카운트된다() {
        LibraryRateLimiter limiter = new LibraryRateLimiter();
        for (int i = 0; i < 30; i++) {
            limiter.tryConsume("203.0.113.1");
        }

        assertThat(limiter.tryConsume("203.0.113.1")).isFalse();
        assertThat(limiter.tryConsume("203.0.113.2")).isTrue();
    }
}
