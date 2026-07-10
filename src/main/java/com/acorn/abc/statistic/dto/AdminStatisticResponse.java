package com.acorn.abc.statistic.dto;

import com.acorn.abc.statistic.domain.AdminStatistic;

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
    public static AdminStatisticResponse from(AdminStatistic statistic) {
        return new AdminStatisticResponse(
                statistic.getPeriodType(),
                statistic.getAgeBand(),
                statistic.getPeriodStartDate(),
                statistic.getPeriodEndDate(),
                statistic.getTotalMemberCount(),
                statistic.getSanctionedMemberCount(),
                statistic.getActiveBookCount(),
                statistic.getTotalRentalCount(),
                statistic.getFreeRentalCount(),
                statistic.getPaidRentalCount(),
                statistic.getTotalReadBookCount(),
                statistic.getTotalPaymentAmount(),
                statistic.getReviewCount(),
                statistic.getReportCount(),
                statistic.getUpdatedAt()
        );
    }
}
