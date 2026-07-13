package com.acorn.abc.statistic.domain;

import java.util.Set;

/**
 * 관리자 감사 로그 action_type 표준값(구현계획 §6.6 1차 표준값 기준).
 * 문자열 난립을 막기 위한 상수 모음이며, DB에는 문자열 그대로 저장된다.
 */
public final class AdminAuditLogActionType {

    private AdminAuditLogActionType() {
    }

    public static final String MEMBER_POINT_ADJUST = "MEMBER_POINT_ADJUST";
    public static final String COUPON_CREATE = "COUPON_CREATE";
    public static final String COUPON_ISSUE = "COUPON_ISSUE";
    public static final String BOOK_CREATE = "BOOK_CREATE";
    public static final String CHALLENGE_UPDATE = "CHALLENGE_UPDATE";

    // 구현계획 §6.6 1차 표준값 중 아직 대응하는 관리자 변경 API가 없는 항목.
    // 해당 API가 구현되는 시점에 실제로 기록 연동한다(§9 후속 작업).
    public static final String MEMBER_UPDATE = "MEMBER_UPDATE";
    public static final String BOOK_UPDATE = "BOOK_UPDATE";
    public static final String REPORT_PROCESS = "REPORT_PROCESS";
    public static final String NOTICE_WRITE = "NOTICE_WRITE";

    private static final Set<String> VALUES = Set.of(
            MEMBER_POINT_ADJUST, COUPON_CREATE, COUPON_ISSUE, BOOK_CREATE, CHALLENGE_UPDATE,
            MEMBER_UPDATE, BOOK_UPDATE, REPORT_PROCESS, NOTICE_WRITE);

    // 관리자API후속작업 구현계획 §7 안 B: 컨트롤러 @Pattern 대신 이 목록으로 검증한다.
    // 상수가 늘어나도(§9 후속 작업) 이 클래스만 고치면 검증도 같이 늘어난다.
    public static boolean isValid(String value) {
        return VALUES.contains(value);
    }
}
