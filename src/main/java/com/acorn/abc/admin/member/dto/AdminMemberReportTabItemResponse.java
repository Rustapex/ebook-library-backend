package com.acorn.abc.admin.member.dto;

import java.time.LocalDateTime;

// reportSourceType: BOOK_REPORT / REVIEW_REPORT — book_report와 review_report를 합쳐 하나의 탭으로 제공한다(구현계획 §1.3 REPORTS).
public record AdminMemberReportTabItemResponse(
        Long reportId,
        String reportSourceType,
        String reportType,
        String status,
        LocalDateTime processedAt,
        LocalDateTime createdAt
) {
}
