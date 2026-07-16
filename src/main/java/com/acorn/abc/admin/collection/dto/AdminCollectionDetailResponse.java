package com.acorn.abc.admin.collection.dto;

import com.acorn.abc.collection.domain.Collection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminCollectionDetailResponse(
        Long collectionId,
        String name,
        String collectionType,
        BigDecimal discountRate,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        Long createdByMemberId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<AdminCollectionBookResponse> books
) {
    public static AdminCollectionDetailResponse of(Collection collection, List<AdminCollectionBookResponse> books) {
        return new AdminCollectionDetailResponse(
                collection.getCollectionId(),
                collection.getName(),
                collection.getCollectionType(),
                collection.getDiscountRate(),
                collection.getDescription(),
                collection.getStartDate(),
                collection.getEndDate(),
                collection.getStatus(),
                collection.getCreatedBy() == null ? null : collection.getCreatedBy().getMemberId(),
                collection.getCreatedAt(),
                collection.getUpdatedAt(),
                books);
    }
}
