package com.acorn.abc.admin.report.dto;

import java.time.LocalDateTime;

// 이번 신고 API에서 허용하는 제재 유형은 ACCOUNT_SUSPENSION 하나뿐이다(2026-07-22 구현계획 §2.4).
public record AdminReportSanctionRequest(
        String reason,
        LocalDateTime startedAt,
        LocalDateTime endedAt
) {}
