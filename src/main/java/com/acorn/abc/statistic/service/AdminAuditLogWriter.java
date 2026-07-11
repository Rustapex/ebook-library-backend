package com.acorn.abc.statistic.service;

import com.acorn.abc.member.domain.Member;
import com.acorn.abc.member.repository.MemberRepository;
import com.acorn.abc.statistic.domain.AdminAuditLog;
import com.acorn.abc.statistic.repository.AdminAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 변경성 API의 부수 효과로 admin_audit_log에 기록을 남긴다(구현계획 §6.6).
 * 별도 트랜잭션을 새로 열지 않고 호출자의 트랜잭션에 그대로 참여한다 — 로그 기록이 실패하면
 * 본 작업도 함께 롤백된다는 정책(§6.6 "로그 기록 실패 정책")을 별도 처리 없이 만족시키기 위함이다.
 */
@Service
@RequiredArgsConstructor
public class AdminAuditLogWriter {

    private final AdminAuditLogRepository adminAuditLogRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public void record(Long adminMemberId, String actionType, String targetType, Long targetId,
                        String beforeSummary, String afterSummary) {
        // admin_audit_log.admin_member_id는 스키마상 NULL 허용이라, adminMemberId가 없을 때도 기록은 남긴다.
        Member adminMember = adminMemberId == null ? null : memberRepository.getReferenceById(adminMemberId);
        AdminAuditLog log = AdminAuditLog.create(adminMember, actionType, targetType, targetId, beforeSummary, afterSummary);
        adminAuditLogRepository.save(log);
    }
}
