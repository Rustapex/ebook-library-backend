package com.acorn.abc.member.dto;

import com.acorn.abc.common.response.PageResponse;

public record PointSummaryResponse(
        int pointBalance,
        PageResponse<PointHistoryResponse> history
) {
}
