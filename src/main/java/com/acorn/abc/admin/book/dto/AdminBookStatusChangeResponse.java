package com.acorn.abc.admin.book.dto;

public record AdminBookStatusChangeResponse(
        Long bookId,
        String status
) {
}
