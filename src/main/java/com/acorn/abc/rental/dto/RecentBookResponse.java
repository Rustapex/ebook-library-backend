package com.acorn.abc.rental.dto;

import java.time.LocalDateTime;

public record RecentBookResponse(
        Long rentalId,
        Long bookId,
        String title,
        String coverImageUrl,
        int currentPage,
        int totalPages,
        double progressRate,
        LocalDateTime lastReadAt
) {
}
