package com.acorn.abc.admin.member.dto;

import java.time.LocalDateTime;

public record AdminMemberPaymentTabItemResponse(
        Long paymentId,
        Long rentalId,
        Long bookId,
        String bookTitle,
        String paymentType,
        int amount,
        String paymentMethod,
        String paymentStatus,
        LocalDateTime paidAt
) {
}
