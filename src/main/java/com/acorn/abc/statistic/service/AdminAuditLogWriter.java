package com.acorn.abc.statistic.service;

import com.acorn.abc.member.domain.Member;
import com.acorn.abc.member.repository.MemberRepository;
import com.acorn.abc.statistic.domain.AdminAuditLog;
import com.acorn.abc.statistic.domain.AdminAuditLogActionType;
import com.acorn.abc.statistic.domain.AdminAuditLogTargetType;
import com.acorn.abc.statistic.repository.AdminAuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    // Spring Boot 4(Jackson 3) 환경이라 com.fasterxml.jackson.databind.ObjectMapper는 자동 구성 빈으로 제공되지 않는다
    // (JacksonAutoConfiguration이 tools.jackson.databind.json.JsonMapper만 등록함).
    // NaverBookClient/AladinBookClient/InfoNaruBookClient와 동일하게 직접 인스턴스를 든다.
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AdminAuditLogRepository adminAuditLogRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public void record(Long adminMemberId, AdminAuditLogActionType actionType, AdminAuditLogTargetType targetType, Long targetId,
                        Object beforeSummary, Object afterSummary) {
        // admin_audit_log.admin_member_id는 스키마상 NULL 허용이라, adminMemberId가 없을 때도 기록은 남긴다.
        Member adminMember = adminMemberId == null ? null : memberRepository.getReferenceById(adminMemberId);
        AdminAuditLog log = AdminAuditLog.create(adminMember, actionType, targetType, targetId,
                toJson(beforeSummary), toJson(afterSummary));
        adminAuditLogRepository.save(log);
    }

    // 호출부는 Map.of(...)/record 같은 구조화된 값을 넘기고, 여기서만 JSON 문자열로 직렬화한다.
    // 문자열을 직접 이어붙이던 방식과 달리 따옴표/역슬래시/줄바꿈 이스케이프를 Jackson에 맡길 수 있다.
    private String toJson(Object summary) {
        if (summary == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(summary);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("감사 로그 summary 직렬화에 실패했습니다.", e);
        }
    }
}
