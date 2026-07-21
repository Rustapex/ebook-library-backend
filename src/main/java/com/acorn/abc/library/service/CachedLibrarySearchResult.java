package com.acorn.abc.library.service;

import com.acorn.abc.external.library.dto.InfoNaruLibrarySearchResult;

import java.time.Instant;

// cache value는 거리 계산 이전 정보나루 원본 결과만 저장한다(계획 3.2). distanceKm은 매 요청 계산한다.
public record CachedLibrarySearchResult(
        InfoNaruLibrarySearchResult result,
        Instant fetchedAt
) {
}
