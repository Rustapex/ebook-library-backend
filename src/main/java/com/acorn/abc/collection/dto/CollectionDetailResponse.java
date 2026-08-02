package com.acorn.abc.collection.dto;

import com.acorn.abc.book.dto.BookCardResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CollectionDetailResponse(
        Long collectionId,
        String collectionName,
        String collectionType,
        BigDecimal discountRate,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        List<BookCardResponse> books
) {
}
