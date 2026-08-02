package com.acorn.abc.rental.dto;

import java.time.LocalDateTime;

public record RecentBookResponse(
        Long rentalId,
        Long bookId,
        String title,
        String coverImageUrl,
        double progressRate,
        boolean completedYn,
        LocalDateTime lastReadAt
) {
}
