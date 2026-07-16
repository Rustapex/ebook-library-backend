package com.acorn.abc.admin.member.dto;

import com.acorn.abc.common.response.PageResponse;

// 회원 상세 이력 탭 공통 응답(구현계획 §1.3) — memberId와 해당 탭 데이터만 포함하고 profile 필드는 구조 자체에 없다.
public record AdminMemberTabResponse<T>(
        Long memberId,
        PageResponse<T> tab
) {
}
