package com.acorn.abc.rental.dto;

public record ViewerPageResponse(
        String title,
        String pageContent,
        int totalPages,
        double progressRate,
        boolean bookmarkYn
) {
}
