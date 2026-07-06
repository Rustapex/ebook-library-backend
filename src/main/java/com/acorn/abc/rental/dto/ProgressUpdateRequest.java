package com.acorn.abc.rental.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ProgressUpdateRequest(
        @NotNull(message = "현재 페이지는 필수입니다.")
        @Positive(message = "현재 페이지는 1 이상이어야 합니다.")
        Integer currentPage,

        String action
) {
}
