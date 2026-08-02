package com.acorn.abc.request.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BookRequestHistoryItemResponse(
        Long requestId,
        String title,
        String author,
        String publisher,
        String reason,
        LocalDateTime createdAt,
        String status,
        int requestCount,
        Long approvedBookId,
        String rejectReason
) {
}
