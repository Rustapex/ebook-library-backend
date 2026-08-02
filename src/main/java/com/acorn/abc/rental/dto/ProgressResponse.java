package com.acorn.abc.rental.dto;

public record ProgressResponse(
        Long progressId,
        int epubVersion,
        String cfi,
        String chapterHref,
        Integer spineIndex,
        double progression,
        long revision,
        boolean completedYn
) {
}
