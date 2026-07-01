package com.acorn.abc.book.dto;

import com.acorn.abc.common.response.PageResponse;

public record BookSearchResponse(
        String keyword,
        long totalCount,
        PageResponse<BookCardResponse> page
) {
}
