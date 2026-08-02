package com.acorn.abc.admin.collection.dto;

import com.acorn.abc.collection.domain.Collection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminCollectionSummaryResponse(
        Long collectionId,
        String name,
        String collectionType,
        BigDecimal discountRate,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        long mappedCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminCollectionSummaryResponse of(Collection collection, long mappedCount) {
        return new AdminCollectionSummaryResponse(
                collection.getCollectionId(),
                collection.getName(),
                collection.getCollectionType(),
                collection.getDiscountRate(),
                collection.getStartDate(),
                collection.getEndDate(),
                collection.getStatus(),
                mappedCount,
                collection.getCreatedAt(),
                collection.getUpdatedAt());
    }
}
