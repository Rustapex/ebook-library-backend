package com.acorn.abc.book.dto;

import java.util.List;

public record BookRecommendationResponse(
        String recommendationType,
        List<BookCardResponse> books
) {
}
