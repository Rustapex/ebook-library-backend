package com.acorn.abc.statistic.domain;

/**
 * 관리자 감사 로그 target_type 표준값(구현계획 §6.6 1차 표준값 기준).
 * DB(admin_audit_log.target_type)에는 @Enumerated(EnumType.STRING)으로 이름 그대로 저장된다.
 */
public enum AdminAuditLogTargetType {

    MEMBER,
    COUPON,
    BOOK,
    CHALLENGE,

    // 아직 이 target에 쓰는 관리자 변경 API가 없는 항목(§9 후속 작업 대상).
    BOOK_REPORT,
    REVIEW_REPORT,
    REVIEW,
    NOTICE,
    SANCTION_HISTORY
}
