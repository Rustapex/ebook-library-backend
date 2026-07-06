package com.acorn.abc.coupon.dto;

import java.time.LocalDateTime;

public record MyCouponResponse(
        Long memberCouponId,
        String couponName,
        String couponType,
        int benefitValue,
        String benefitUnit,
        String couponStatus,
        LocalDateTime issuedAt,
        LocalDateTime expiresAt,
        LocalDateTime usedAt,
        boolean expiringYn
) {
}
