package com.acorn.abc.member.dto;

import com.acorn.abc.member.domain.Member;

import java.time.LocalDate;

public record MeInfoResponse(
        Long memberId,
        String loginId,
        String name,
        String email,
        String phone,
        LocalDate birthDate,
        String gender,
        String role,
        String status,
        MemberGradeSummaryResponse grade,
        int pointBalance,
        long couponCount,
        long rentalCount,
        long completedBookCount,
        long favoriteCount,
        long unreadNotificationCount
) {
    public static MeInfoResponse of(Member member, MemberGradeSummaryResponse grade, long couponCount,
                                     long rentalCount, long completedBookCount, long favoriteCount,
                                     long unreadNotificationCount) {
        return new MeInfoResponse(
                member.getMemberId(),
                member.getLoginId(),
                member.getName(),
                member.getEmail(),
                member.getPhone(),
                member.getBirthDate(),
                member.getGender(),
                member.getRole(),
                member.getStatus(),
                grade,
                member.getPointBalance(),
                couponCount,
                rentalCount,
                completedBookCount,
                favoriteCount,
                unreadNotificationCount
        );
    }
}
