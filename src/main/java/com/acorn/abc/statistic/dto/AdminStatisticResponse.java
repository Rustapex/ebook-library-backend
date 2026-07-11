package com.acorn.abc.statistic.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminStatisticResponse(
        String periodType,
        String ageBand,
        LocalDate periodStartDate,
        LocalDate periodEndDate,
        int totalMemberCount,
        int sanctionedMemberCount,
        int activeBookCount,
        int totalRentalCount,
        int freeRentalCount,
        int paidRentalCount,
        int totalReadBookCount,
        long totalPaymentAmount,
        int reviewCount,
        int reportCount,
        LocalDateTime updatedAt
) {
}
