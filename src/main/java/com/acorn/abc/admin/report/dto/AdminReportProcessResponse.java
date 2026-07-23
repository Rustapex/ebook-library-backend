package com.acorn.abc.admin.report.dto;

import java.time.LocalDateTime;

public record AdminReportProcessResponse(
        Long reportId,
        String targetType,
        String status,
        String processResult,
        LocalDateTime processedAt,
        String reviewStatus,
        Long sanctionHistoryId,
        String memberStatus
) {}
