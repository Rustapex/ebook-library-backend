package com.acorn.abc.admin.request.dto;

public record AdminBookRequestCandidateCriteria(
        String status,
        String q,
        int page,
        int size
) {
}
