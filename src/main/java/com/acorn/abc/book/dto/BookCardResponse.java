package com.acorn.abc.book.dto;

import java.math.BigDecimal;
import java.util.List;

public record BookCardResponse(
        Long bookId,
        String title,
        String coverImageUrl,
        List<String> authors,
        String publisherName,
        String rentalType,
        Integer rentalPrice,
        BigDecimal averageRating,
        Integer reviewCount,
        Boolean favoriteYn
) {
}
