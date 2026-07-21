package com.acorn.abc.library.service;

// cache key는 좌표(latitude/longitude)를 포함하지 않는다(계획 3.2) — 같은 도서관 목록 조회를
// 사용자 좌표 차이로 별도 cache entry를 만들지 않기 위함이다.
public record LibraryCacheKey(
        String isbn,
        String region,
        String dtlRegion,
        int page,
        int size
) {
}
