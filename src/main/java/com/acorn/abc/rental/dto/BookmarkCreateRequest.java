package com.acorn.abc.rental.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BookmarkCreateRequest(
        @NotNull(message = "페이지 번호는 필수입니다.")
        @Positive(message = "페이지 번호는 1 이상이어야 합니다.")
        Integer pageNo
) {
}
