package com.acorn.abc.statistic.dto;

import com.acorn.abc.payment.domain.Payment;

import java.time.LocalDateTime;

public record AdminRecentPaymentResponse(
        Long paymentId,
        String memberName,
        int amount,
        String paymentStatus,
        LocalDateTime paidAt
) {
    public static AdminRecentPaymentResponse from(Payment payment) {
        return new AdminRecentPaymentResponse(
                payment.getPaymentId(),
                payment.getMember().getName(),
                payment.getAmount(),
                payment.getPaymentStatus(),
                payment.getPaidAt()
        );
    }
}
