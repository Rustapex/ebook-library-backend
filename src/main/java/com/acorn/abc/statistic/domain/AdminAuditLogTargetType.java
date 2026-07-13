package com.acorn.abc.statistic.domain;

import java.util.Set;

/**
 * 관리자 감사 로그 target_type 표준값(구현계획 §6.6 1차 표준값 기준).
 */
public final class AdminAuditLogTargetType {

    private AdminAuditLogTargetType() {
    }

    public static final String MEMBER = "MEMBER";
    public static final String COUPON = "COUPON";
    public static final String BOOK = "BOOK";
    public static final String CHALLENGE = "CHALLENGE";

    // 아직 이 target에 쓰는 관리자 변경 API가 없는 항목(§9 후속 작업 대상).
    public static final String BOOK_REPORT = "BOOK_REPORT";
    public static final String REVIEW_REPORT = "REVIEW_REPORT";
    public static final String REVIEW = "REVIEW";
    public static final String NOTICE = "NOTICE";
    public static final String SANCTION_HISTORY = "SANCTION_HISTORY";

    private static final Set<String> VALUES = Set.of(
            MEMBER, COUPON, BOOK, CHALLENGE, BOOK_REPORT, REVIEW_REPORT, REVIEW, NOTICE, SANCTION_HISTORY);

    // 관리자API후속작업 구현계획 §7 안 B: 컨트롤러 @Pattern 대신 이 목록으로 검증한다.
    public static boolean isValid(String value) {
        return VALUES.contains(value);
    }
}
