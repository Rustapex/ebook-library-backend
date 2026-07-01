package com.acorn.abc.book.dto;

public record BookRentalInfoResponse(
        String rentalType,
        Integer rentalPrice,
        Integer defaultRentalDays
) {
}
