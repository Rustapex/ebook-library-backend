package com.acorn.abc.rental.dto;

import java.time.LocalDateTime;

public record BookmarkResponse(
        Long bookmarkId,
        int pageNo,
        LocalDateTime createdAt
) {
}
