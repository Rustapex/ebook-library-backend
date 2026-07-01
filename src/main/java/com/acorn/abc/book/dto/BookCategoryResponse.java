package com.acorn.abc.book.dto;

public record BookCategoryResponse(
        Long categoryId,
        Long parentCategoryId,
        String categoryName,
        Integer displayOrder
) {
}
