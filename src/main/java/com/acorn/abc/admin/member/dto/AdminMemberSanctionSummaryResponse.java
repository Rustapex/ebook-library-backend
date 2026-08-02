package com.acorn.abc.admin.member.dto;

import java.time.LocalDateTime;

// 목록/상세 공통 — 현재 유효한 제재만의 최소 요약(구현계획 §1.2). 만료된 제재·상세 사유는 포함하지 않는다.
public record AdminMemberSanctionSummaryResponse(
        String sanctionType,
        LocalDateTime endedAt
) {
}
