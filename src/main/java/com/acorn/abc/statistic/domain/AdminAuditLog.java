package com.acorn.abc.statistic.domain;

import com.acorn.abc.member.domain.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_audit_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_log_id")
    private Long auditLogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_member_id")
    private Member adminMember;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private AdminAuditLogActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 50)
    private AdminAuditLogTargetType targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "before_summary", columnDefinition = "TEXT")
    private String beforeSummary;

    @Column(name = "after_summary", columnDefinition = "TEXT")
    private String afterSummary;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static AdminAuditLog create(Member adminMember, AdminAuditLogActionType actionType, AdminAuditLogTargetType targetType, Long targetId,
                                        String beforeSummary, String afterSummary) {
        AdminAuditLog log = new AdminAuditLog();
        log.adminMember = adminMember;
        log.actionType = actionType;
        log.targetType = targetType;
        log.targetId = targetId;
        log.beforeSummary = beforeSummary;
        log.afterSummary = afterSummary;
        log.createdAt = LocalDateTime.now();
        return log;
    }
}
