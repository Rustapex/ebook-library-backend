package com.acorn.abc.auth.dto;

import java.time.LocalDateTime;

public record FindIdResponse(
        boolean found,
        String loginId,
        LocalDateTime joinedAt
) {
    // 앞 2자 노출, 나머지 * 처리
    public static FindIdResponse found(String loginId, LocalDateTime joinedAt) {
        String masked = loginId.substring(0, 2) + "*".repeat(loginId.length() - 2);
        return new FindIdResponse(true, masked, joinedAt);
    }

    public static FindIdResponse notFound() {
        return new FindIdResponse(false, null, null);
    }
}
