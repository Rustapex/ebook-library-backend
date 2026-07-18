package com.acorn.abc.rental.dto;

public record ViewerSessionResponse(
        String title,
        int epubVersion,
        boolean contentReady,
        ViewerProgressResponse progress
) {
    public record ViewerProgressResponse(
            String cfi,
            String chapterHref,
            Integer spineIndex,
            double progression,
            long revision,
            boolean completedYn
    ) {
    }
}
