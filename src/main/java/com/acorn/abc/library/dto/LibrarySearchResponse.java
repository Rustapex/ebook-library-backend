package com.acorn.abc.library.dto;

import java.util.List;

// GET /api/v1/books/{bookId}/libraries 응답. distanceKm은 요청마다 계산하며(계획 3.2),
// 정렬은 "현재 조회된 page 내부"로 한정한다(계획 3.4). source는 INFONARU | MANUAL.
// 조회에 사용하는 ISBN은 공개 응답에 중복 노출하지 않는다.
public record LibrarySearchResponse(
        Long bookId,
        String title,
        String coverImageUrl,
        String region,
        String source,
        boolean stale,
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<LibraryResultItem> libraries,
        List<String> warnings
) {

    public record LibraryResultItem(
            String libraryCode,
            String libraryName,
            String address,
            java.math.BigDecimal latitude,
            java.math.BigDecimal longitude,
            Double distanceKm,
            String homepageUrl,
            String closedDays,
            String operationTime,
            boolean holding,
            Boolean loanAvailable
    ) {
    }
}
