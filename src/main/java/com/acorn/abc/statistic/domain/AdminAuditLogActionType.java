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

    // 관리자 컬렉션 API 구현계획(2026-07-16) D-11 확정.
    COLLECTION_CREATE,
    COLLECTION_UPDATE,
    COLLECTION_BOOKS_UPDATE,
    COLLECTION_PATCH,

    // 구현계획 §6.6 1차 표준값 중 아직 대응하는 관리자 변경 API가 없는 항목.
    // 해당 API가 구현되는 시점에 실제로 기록 연동한다(§9 후속 작업).
    MEMBER_UPDATE,
    BOOK_UPDATE,
    REPORT_PROCESS,
    NOTICE_WRITE,

    // 희망도서/알림/공지 구현계획 — 관리자 후보 상태 변경(승인/반려/검토중 전환) 시 기록.
    BOOK_REQUEST_STATUS_UPDATE
}
