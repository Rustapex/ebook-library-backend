package com.acorn.abc.admin.member.dto;

// 회원 상세 DEFAULT 탭 응답(구현계획 §1.3).
public record AdminMemberDetailResponse(
        Long memberId,
        AdminMemberProfileResponse profile,
        AdminMemberUsageSummaryResponse usageSummary
) {
}
