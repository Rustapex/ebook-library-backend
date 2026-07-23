package com.acorn.abc.rental.dto;

import java.time.LocalDateTime;

public record AdminRentalPaymentSummaryResponse(
        Long paymentId,
        int amount,
        String paymentMethod,
        LocalDateTime paidAt
) {}
