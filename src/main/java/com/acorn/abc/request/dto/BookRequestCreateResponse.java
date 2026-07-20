package com.acorn.abc.request.dto;

public record BookRequestCreateResponse(
        Long requestId,
        Long requestCandidateId,
        String requestStatus,
        int requestCount
) {
}
