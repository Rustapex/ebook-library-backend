package com.acorn.abc.admin.member.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 회원 상세 DEFAULT 탭 프로필(구현계획 §1.3) — 원문 email/phone/birthDate/gender를 포함한다.
public record AdminMemberProfileResponse(
        Long memberId,
        String loginId,
        String name,
        String email,
        String phone,
        LocalDate birthDate,
        String gender,
        String role,
        String status,
        String gradeName,
        LocalDateTime createdAt
) {
}
