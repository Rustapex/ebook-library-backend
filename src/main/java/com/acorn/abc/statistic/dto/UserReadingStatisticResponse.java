package com.acorn.abc.statistic.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record UserReadingStatisticResponse(
        String periodType,
        LocalDate periodStartDate,
        LocalDate periodEndDate,
        long rentalCount,
        long readBookCount,
        long reviewCount,
        long favoriteCount,
        long carbonSavedKg,
        double treeSavedCount,
        List<TrendPoint> readingTrend,
        LocalDateTime updatedAt
) {

    public record TrendPoint(String label, LocalDate periodStartDate, LocalDate periodEndDate, long count) {
    }
}
