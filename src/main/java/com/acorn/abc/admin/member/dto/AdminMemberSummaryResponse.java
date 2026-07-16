package com.acorn.abc.admin.member.dto;

import java.time.LocalDateTime;
import java.util.List;

// 회원 목록(API-ADMIN-MEMBER-001) — birthDate/gender 필드는 구현계획 §1.2에 따라 아예 선언하지 않는다.
public record AdminMemberSummaryResponse(
        Long memberId,
        String loginId,
        String name,
        String maskedEmail,
        String maskedPhone,
        String role,
        String status,
        String gradeName,
        List<AdminMemberSanctionSummaryResponse> activeSanctions,
        LocalDateTime createdAt
) {
}
