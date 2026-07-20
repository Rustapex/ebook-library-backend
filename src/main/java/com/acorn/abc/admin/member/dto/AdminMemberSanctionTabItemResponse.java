package com.acorn.abc.admin.member.dto;

import java.time.LocalDateTime;

public record AdminMemberSanctionTabItemResponse(
        Long sanctionHistoryId,
        String sanctionType,
        String reason,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        LocalDateTime createdAt
) {
}
