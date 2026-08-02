package com.acorn.abc.member.dto;

public record MemberGradeSummaryResponse(
        Long gradeId,
        String gradeName,
        int gradeLevel,
        Long nextGradeId,
        String nextGradeName,
        long currentPaymentAmount,
        Long targetPaymentAmount,
        double progressRate
) {}
