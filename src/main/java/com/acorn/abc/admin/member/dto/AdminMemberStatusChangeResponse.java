package com.acorn.abc.admin.member.dto;

public record AdminMemberStatusChangeResponse(
        Long memberId,
        String status,
        AdminMemberSanctionSummaryResponse currentSanction
) {
}
