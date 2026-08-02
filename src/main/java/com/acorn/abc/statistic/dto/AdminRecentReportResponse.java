package com.acorn.abc.statistic.dto;

import com.acorn.abc.review.domain.BookReport;
import com.acorn.abc.review.domain.ReviewReport;

import java.time.LocalDateTime;

public record AdminRecentReportResponse(
        Long reportId,
        String targetType,
        Long targetId,
        String targetTitle,
        String reportType,
        String reportStatus,
        String reporterName,
        LocalDateTime createdAt
) {
    public static AdminRecentReportResponse fromBookReport(BookReport report) {
        return new AdminRecentReportResponse(
                report.getBookReportId(),
                "BOOK",
                report.getBook().getBookId(),
                report.getBook().getTitle(),
                report.getReportType(),
                report.getStatus(),
                report.getReporter().getName(),
                report.getCreatedAt());
    }

    public static AdminRecentReportResponse fromReviewReport(ReviewReport report) {
        return new AdminRecentReportResponse(
                report.getReviewReportId(),
                "REVIEW",
                report.getReview().getReviewId(),
                report.getReview().getBook().getTitle(),
                report.getReportType(),
                report.getStatus(),
                report.getReporter().getName(),
                report.getCreatedAt());
    }
}
