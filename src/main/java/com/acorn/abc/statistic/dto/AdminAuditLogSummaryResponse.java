package com.acorn.abc.statistic.dto;

import com.acorn.abc.statistic.domain.AdminAuditLog;

import java.time.LocalDateTime;

public record AdminAuditLogSummaryResponse(
        Long auditLogId,
        Long adminMemberId,
        String adminMemberName,
        String actionType,
        String targetType,
        Long targetId,
        String beforeSummary,
        String afterSummary,
        LocalDateTime createdAt
) {
    public static AdminAuditLogSummaryResponse from(AdminAuditLog log) {
        return new AdminAuditLogSummaryResponse(
                log.getAuditLogId(),
                log.getAdminMember() != null ? log.getAdminMember().getMemberId() : null,
                log.getAdminMember() != null ? log.getAdminMember().getName() : null,
                log.getActionType().name(),
                log.getTargetType().name(),
                log.getTargetId(),
                log.getBeforeSummary(),
                log.getAfterSummary(),
                log.getCreatedAt()
        );
    }
}
