package com.acorn.abc.rental.dto;

public record ProgressResponse(
        Long progressId,
        int currentPage,
        double progressRate,
        boolean completedYn
) {
}
