package com.acorn.abc.library.config;

import com.acorn.abc.library.service.CachedLibrarySearchResult;
import com.acorn.abc.library.service.LibraryCacheKey;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

// fetchedAt 기반 fresh/stale 판정이 필요해 @Cacheable 대신 Caffeine Cache를 직접 주입한다(계획 5.3).
@Configuration
@EnableCaching
public class LibraryCacheConfig {

    private static final int MAXIMUM_SIZE = 5000;

    @Bean
    public Cache<LibraryCacheKey, CachedLibrarySearchResult> libraryHoldingCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofHours(24))
                .maximumSize(MAXIMUM_SIZE)
                .build();
    }
}
