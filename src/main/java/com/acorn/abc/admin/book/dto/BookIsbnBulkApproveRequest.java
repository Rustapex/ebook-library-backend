package com.acorn.abc.admin.book.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BookIsbnBulkApproveRequest(
        @Size(max = 100, message = "tempIds accepts at most 100 entries") List<Long> tempIds,
        @Min(value = 1, message = "limit must be at least 1")
        @Max(value = 30, message = "limit must be at most 30") Integer limit,
        @Min(value = 1, message = "maxCandidates must be at least 1")
        @Max(value = 100, message = "maxCandidates must be at most 100") Integer maxCandidates,
        @NotNull(message = "rentalType is required")
        @Pattern(regexp = "FREE|PAID", message = "rentalType must be FREE or PAID") String rentalType,
        @NotNull(message = "rentalPrice is required")
        @Min(value = 0, message = "rentalPrice must be zero or greater") Integer rentalPrice,
        @NotBlank(message = "status is required")
        @Pattern(regexp = "AVAILABLE|HIDDEN|INACTIVE", message = "invalid book status") String status
) {
    private static final int DEFAULT_LIMIT = 30;
    private static final int DEFAULT_MAX_CANDIDATES = 100;

    public BookIsbnBulkApproveRequest {
        if (limit == null) {
            limit = DEFAULT_LIMIT;
        }
        if (maxCandidates == null) {
            maxCandidates = DEFAULT_MAX_CANDIDATES;
        }
    }
}
