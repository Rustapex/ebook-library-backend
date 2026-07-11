package com.acorn.abc.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReviewRequest(
        @NotNull(message = "도서 번호는 필수입니다.")
        Long bookId,

        @NotNull(message = "별점은 필수입니다.")
        @Min(value = 1, message = "별점은 1 이상이어야 합니다.")
        @Max(value = 5, message = "별점은 5 이하여야 합니다.")
        Integer ratingScore,

        @NotBlank(message = "리뷰 내용은 필수입니다.")
        String content
) {
}
