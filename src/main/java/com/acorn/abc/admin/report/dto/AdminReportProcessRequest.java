package com.acorn.abc.admin.report.dto;

import jakarta.validation.constraints.NotBlank;

// processResult/hideReviewYn/sanction의 상태 의존적 필수 여부(최종 처리일 때만 등)는 어노테이션으로 표현할 수 없어
// 서비스에서 검증한다(2026-07-22 구현계획 §2.4, AdminMemberStatusChangeRequest와 동일한 패턴).
public record AdminReportProcessRequest(
        @NotBlank(message = "처리 상태는 필수입니다.")
        String status,

        String processResult,

        Boolean hideReviewYn,

        AdminReportSanctionRequest sanction
) {}
