package com.acorn.abc.admin.request.dto;

import java.time.LocalDateTime;

public record AdminBookRequestApplicantResponse(
        Long memberId,
        String loginId,
        String name,
        String reason,
        LocalDateTime requestedAt
) {
}
