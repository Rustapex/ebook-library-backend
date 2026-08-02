package com.acorn.abc.book.dto;

public record BookSearchCriteria(
        String keyword,
        Long categoryId,
        String rentalType,
        String sort,
        int page,
        int size
) {
}
