package com.acorn.abc.review.dto;

import com.acorn.abc.book.dto.BookReviewSummaryResponse;
import com.acorn.abc.common.response.PageResponse;

public record ReviewListResponse(
        BookReviewSummaryResponse summary,
        PageResponse<ReviewResponse> reviews
) {
}
