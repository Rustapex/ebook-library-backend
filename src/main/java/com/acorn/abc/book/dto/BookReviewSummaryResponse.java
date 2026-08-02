package com.acorn.abc.book.dto;

import java.math.BigDecimal;

public record BookReviewSummaryResponse(
        BigDecimal averageRating,
        Integer reviewCount
) {
}
