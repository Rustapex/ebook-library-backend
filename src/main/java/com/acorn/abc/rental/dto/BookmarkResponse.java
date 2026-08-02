package com.acorn.abc.rental.dto;

import java.time.LocalDateTime;

public record BookmarkResponse(
        Long bookmarkId,
        int epubVersion,
        String cfi,
        String chapterHref,
        Integer spineIndex,
        double progression,
        LocalDateTime createdAt
) {
}
