package com.acorn.abc.statistic.dto;

import com.acorn.abc.request.domain.BookRequestCandidate;

import java.time.LocalDateTime;

public record AdminRecentBookRequestResponse(
        Long candidateId,
        String title,
        String author,
        String publisher,
        int requestCount,
        String candidateStatus,
        LocalDateTime firstRequestedAt
) {
    public static AdminRecentBookRequestResponse from(BookRequestCandidate candidate) {
        return new AdminRecentBookRequestResponse(
                candidate.getRequestCandidateId(),
                candidate.getNormalizedTitle(),
                candidate.getNormalizedAuthor(),
                candidate.getNormalizedPublisher(),
                candidate.getRequestCount(),
                candidate.getCandidateStatus(),
                candidate.getFirstRequestedAt());
    }
}
