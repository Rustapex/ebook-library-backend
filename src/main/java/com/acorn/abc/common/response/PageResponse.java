package com.acorn.abc.common.response;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        boolean last = totalPages == 0 || page >= totalPages - 1;
        return new PageResponse<>(content, page, size, totalElements, totalPages, last);
    }
}
