package com.acorn.abc.admin.book.dto;

import java.util.List;

public record BookIsbnBulkApproveResponse(
        int approvedCount,
        List<Long> approvedBookIds,
        List<Failure> failures
) {

    public record Failure(String isbn13, String reason) {
    }
}
