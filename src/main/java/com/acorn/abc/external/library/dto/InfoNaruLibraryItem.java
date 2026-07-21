package com.acorn.abc.external.library.dto;

import java.math.BigDecimal;

// 정보나루 libSrchByBook 응답 항목 하나. loanAvailable은 정보나루가 제공하지 않으므로 포함하지 않는다
// (계획 3.3 — 소장과 대출 가능은 구분한다).
public record InfoNaruLibraryItem(
        String libraryCode,
        String libraryName,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String homepageUrl,
        String closedDays,
        String operationTime
) {
}
