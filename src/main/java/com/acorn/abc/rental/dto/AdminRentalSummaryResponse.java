package com.acorn.abc.rental.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// payment: 결제 없는 대여(FREE 대여)는 nullable(2026-07-22 구현계획 §2.3).
public record AdminRentalSummaryResponse(
        Long rentalId,
        Long memberId,
        String memberName,
        Long bookId,
        String bookTitle,
        String rentalStatus,
        LocalDateTime createdAt,
        LocalDateTime rentalStartAt,
        LocalDateTime rentalEndAt,
        BigDecimal progression,
        AdminRentalPaymentSummaryResponse payment
) {}
