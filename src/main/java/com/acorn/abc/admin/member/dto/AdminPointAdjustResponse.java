package com.acorn.abc.admin.member.dto;

import java.time.LocalDateTime;

public record AdminPointAdjustResponse(
        Long pointHistoryId,
        int pointAmount,
        String pointType,
        int pointBalance,
        LocalDateTime createdAt
) {
}
