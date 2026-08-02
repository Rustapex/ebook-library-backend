package com.acorn.abc.book.dto;

public record BookListCriteria(
        Long categoryId,
        Long parentCategoryId,
        Long collectionId,
        String rentalType,
        String section,
        String sort,
        int page,
        int size
) {
}
