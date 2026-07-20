package com.acorn.abc.admin.request.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdminBookRequestStatusUpdateResponse(
        Long candidateId,
        String status,
        int affectedRequestCount,
        Long approvedBookId,
        String rejectReason
) {
}
