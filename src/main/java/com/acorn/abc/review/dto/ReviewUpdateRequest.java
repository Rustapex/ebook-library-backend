package com.acorn.abc.review.dto;

import jakarta.validation.constraints.NotBlank;

public record ReviewUpdateRequest(
        @NotBlank(message = "리뷰 내용은 필수입니다.")
        String content
) {
}
