package com.acorn.abc.admin.coupon.dto;

public record AdminCouponSummaryResponse(
        Long couponId,
        String couponName,
        String couponType,
        int benefitValue,
        String benefitUnit,
        int validDays,
        String status,
        long issuedCount,
        long usedCount,
        long expiredCount
) {
}
