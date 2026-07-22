package com.acorn.abc.payment.dto;

import java.time.LocalDateTime;

public record AdminPaymentSummaryResponse(
        Long paymentId,
        Long memberId,
        String memberName,
        Long rentalId,
        Long bookId,
        String bookTitle,
        String paymentMethod,
        int originalAmount,
        int couponDiscountAmount,
        int pointUsedAmount,
        int amount,
        LocalDateTime paidAt
) {}
