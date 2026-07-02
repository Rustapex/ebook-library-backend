package com.acorn.abc.auth.dto;

import com.acorn.abc.member.domain.Member;

public record SignupResponse(
        Long memberId,
        String loginId,
        String name,
        String email,
        String role,
        String status
) {
    public static SignupResponse from(Member member) {
        return new SignupResponse(
                member.getMemberId(),
                member.getLoginId(),
                member.getName(),
                member.getEmail(),
                member.getRole(),
                member.getStatus()
        );
    }
}
