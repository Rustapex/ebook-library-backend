package com.acorn.abc.statistic.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
        LocalDateTime updatedAt
) {
}
