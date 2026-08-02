package com.acorn.abc.admin.book.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * PENDING temp 후보를 외부 API로 보강(enrich)하는 요청.
 */
public record BookIsbnEnrichRequest(
        @NotNull(message = "처리 개수는 필수입니다.")
        @Min(value = 1, message = "처리 개수는 1 이상이어야 합니다.")
        Integer limit
) {
}
