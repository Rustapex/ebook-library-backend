package com.acorn.abc.admin.book.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * READY temp 후보를 일괄 승인하는 요청.
 * tempIds가 비어 있으면 READY 전체 중 자동 승인 조건을 만족하는 후보가 대상이 된다.
 * limit(승인시킬 최대 책 수)과 maxCandidates(검사할 최대 temp 수)를 분리해 무제한 승인/무제한 스캔을 모두 막는다.
 */
public record BookIsbnBulkApproveRequest(
        @Size(max = 100, message = "tempIds는 최대 100개까지 지정할 수 있습니다.")
        List<Long> tempIds,

        @Min(value = 1, message = "승인 개수 제한은 1 이상이어야 합니다.")
        @Max(value = 30, message = "승인 개수 제한은 30 이하여야 합니다.")
        Integer limit,

        @Min(value = 1, message = "검사 후보 수 제한은 1 이상이어야 합니다.")
        @Max(value = 100, message = "검사 후보 수 제한은 100 이하여야 합니다.")
        Integer maxCandidates,

        @NotNull(message = "대여 타입은 필수입니다.")
        @Pattern(regexp = "FREE|PAID", message = "대여 타입은 FREE 또는 PAID여야 합니다.")
        String rentalType,

        @NotNull(message = "대여 가격은 필수입니다.")
        @Min(value = 0, message = "대여 가격은 0 이상이어야 합니다.")
        Integer rentalPrice,

        @NotBlank(message = "도서 상태는 필수입니다.")
        @Pattern(regexp = "AVAILABLE|HIDDEN|INACTIVE", message = "도서 상태가 올바르지 않습니다.")
        String status,

        @NotNull(message = "더미 페이지 수는 필수입니다.")
        @Min(value = 1, message = "더미 페이지 수는 1 이상이어야 합니다.")
        @Max(value = 10, message = "더미 페이지 수는 10 이하여야 합니다.")
        Integer dummyPageCount
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
