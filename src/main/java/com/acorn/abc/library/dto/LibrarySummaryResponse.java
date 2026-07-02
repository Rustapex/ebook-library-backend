package com.acorn.abc.library.dto;

import java.math.BigDecimal;

public record LibrarySummaryResponse(
        Long bookId,
        String title,
        String libraryName,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String holdingStatus
) {
}
