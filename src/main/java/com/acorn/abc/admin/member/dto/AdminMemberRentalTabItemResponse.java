package com.acorn.abc.admin.member.dto;

import java.time.LocalDateTime;

public record AdminMemberRentalTabItemResponse(
        Long rentalId,
        Long bookId,
        String bookTitle,
        String rentalStatus,
        LocalDateTime rentalStartAt,
        LocalDateTime rentalEndAt,
        LocalDateTime createdAt
) {
}
