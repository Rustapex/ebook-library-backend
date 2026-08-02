package com.acorn.abc.review.dto;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long reviewId,
        Long ratingId,
        Long bookId,
        Long memberId,
        String memberName,
        Integer ratingScore,
        String content,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
