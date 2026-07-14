package com.acorn.abc.statistic.domain;

/**
 * 관리자 감사 로그 action_type 표준값(구현계획 §6.6 1차 표준값 기준).
 * DB(admin_audit_log.action_type)에는 @Enumerated(EnumType.STRING)으로 이름 그대로 저장된다.
 */
public enum AdminAuditLogActionType {

    MEMBER_POINT_ADJUST,
    COUPON_CREATE,
    COUPON_ISSUE,
    BOOK_CREATE,
    CHALLENGE_UPDATE,

    // 구현계획 §6.6 1차 표준값 중 아직 대응하는 관리자 변경 API가 없는 항목.
    // 해당 API가 구현되는 시점에 실제로 기록 연동한다(§9 후속 작업).
    MEMBER_UPDATE,
    BOOK_UPDATE,
    REPORT_PROCESS,
    NOTICE_WRITE
}
