package com.acorn.abc.rental.dto;

import java.time.LocalDateTime;

public record MyRentalSummaryResponse(
        Long rentalId,
        Long bookId,
        String title,
        String coverImageUrl,
        String rentalStatus,
        LocalDateTime rentalEndAt,
        double progressRate,
        boolean completedYn
) {
}
