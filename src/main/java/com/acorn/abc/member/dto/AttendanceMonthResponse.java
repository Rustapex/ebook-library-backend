package com.acorn.abc.member.dto;

import java.time.LocalDate;
import java.util.List;

public record AttendanceMonthResponse(
        List<LocalDate> attendanceDates,
        boolean todayCheckedYn,
        int consecutiveDays
) {
}
