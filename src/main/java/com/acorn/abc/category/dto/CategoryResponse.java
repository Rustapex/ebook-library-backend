package com.acorn.abc.category.dto;

import java.util.List;

public record CategoryResponse(
        Long categoryId,
        Long parentCategoryId,
        String categoryName,
        Integer displayOrder,
        List<CategoryResponse> children
) {
}
