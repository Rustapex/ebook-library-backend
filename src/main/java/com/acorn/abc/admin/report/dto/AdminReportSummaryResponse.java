package com.acorn.abc.admin.report.dto;

import com.acorn.abc.admin.report.repository.AdminReportRepository;

import java.time.LocalDateTime;

// reviewId/reviewContent: targetType=REVIEW일 때만 채워진다(2026-07-22 구현계획 §2.4). 별도 상세 API 없이
// 이 목록 행만으로 모달에 신고자·대상·신고 내용을 표시한다.
public record AdminReportSummaryResponse(
        Long reportId,
        String targetType,
        Long reporterId,
        String reporterName,
        Long bookId,
        String bookTitle,
        Long reviewId,
        String reviewContent,
        String reportType,
        String content,
        String status,
        String processResult,
        LocalDateTime processedAt,
        LocalDateTime createdAt
) {
    public static AdminReportSummaryResponse from(AdminReportRepository.AdminReportRow row) {
        return new AdminReportSummaryResponse(
                row.getReportId(), row.getTargetType(), row.getReporterId(), row.getReporterName(),
                row.getBookId(), row.getBookTitle(), row.getReviewId(), row.getReviewContent(),
                row.getReportType(), row.getContent(), row.getStatus(), row.getProcessResult(),
                row.getProcessedAt(), row.getCreatedAt());
    }
}
