package com.acorn.abc.member.dto;

import java.time.LocalDate;
import java.util.List;

public record AttendanceCheckResponse(
        LocalDate attendanceDate,
        int consecutiveDays,
        List<IssuedCouponResponse> issuedCoupons
) {
}
