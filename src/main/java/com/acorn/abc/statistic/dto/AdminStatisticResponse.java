package com.acorn.abc.statistic.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminStatisticResponse(
        String periodType,
        String ageBand,
        LocalDate periodStartDate,
        LocalDate periodEndDate,
        long totalMemberCount,
        long sanctionedMemberCount,
        long activeBookCount,
        long totalRentalCount,
        long freeRentalCount,
        long paidRentalCount,
        long totalReadBookCount,
        long totalPaymentAmount,
        long reviewCount,
        long reportCount,
        long carbonSavedKg,
        double treeSavedCount,
        List<TrendCountPoint> rentalTrend,
        List<TrendAmountPoint> paymentTrend,
        List<TrendCountPoint> completedReadTrend,
        LocalDateTime updatedAt
) {

    public record TrendCountPoint(String label, LocalDate periodStartDate, LocalDate periodEndDate, long count) {
    }

    public record TrendAmountPoint(String label, LocalDate periodStartDate, LocalDate periodEndDate, long amount) {
    }
}
