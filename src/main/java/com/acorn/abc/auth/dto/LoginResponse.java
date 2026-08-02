package com.acorn.abc.auth.dto;

import com.acorn.abc.member.domain.Member;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        MemberInfo member
) {
    public record MemberInfo(
            Long memberId,
            String loginId,
            String name,
            String role
    ) {}

    public static LoginResponse of(String accessToken, long expiresIn, Member member) {
        return new LoginResponse(
                accessToken,
                "Bearer",
                expiresIn,
                new MemberInfo(
                        member.getMemberId(),
                        member.getLoginId(),
                        member.getName(),
                        member.getRole()
                )
        );
    }
}
