package com.acorn.abc.admin.member.dto;

import java.util.List;

// 회원 상세 DEFAULT 탭 이용 요약(구현계획 §1.3) — 탭별 상세 데이터는 각 탭 API에서 페이지로 제공한다.
public record AdminMemberUsageSummaryResponse(
        int pointBalance,
        long rentalCount,
        long paymentCount,
        long reportCount,
        List<AdminMemberSanctionSummaryResponse> activeSanctions
) {
}
