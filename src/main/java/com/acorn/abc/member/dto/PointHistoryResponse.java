package com.acorn.abc.member.dto;

import java.time.LocalDateTime;

public record PointHistoryResponse(
        Long pointHistoryId,
        String pointType,
        int pointAmount,
        String description,
        LocalDateTime createdAt
) {
}
