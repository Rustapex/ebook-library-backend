package com.acorn.abc.request.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "book_request_candidate")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookRequestCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_candidate_id")
    private Long requestCandidateId;

    @Column(name = "normalized_title", nullable = false, length = 255)
    private String normalizedTitle;

    @Column(name = "normalized_author", nullable = false, length = 100)
    private String normalizedAuthor;

    @Column(name = "normalized_publisher", nullable = false, length = 100)
    private String normalizedPublisher;

    @Column(name = "request_count", nullable = false)
    private int requestCount;

    @Column(name = "first_requested_at", nullable = false)
    private LocalDateTime firstRequestedAt;

    @Column(name = "candidate_status", nullable = false, length = 20)
    private String candidateStatus;

    public static BookRequestCandidate createNew(String normalizedTitle, String normalizedAuthor, String normalizedPublisher,
                                                   LocalDateTime firstRequestedAt) {
        BookRequestCandidate candidate = new BookRequestCandidate();
        candidate.normalizedTitle = normalizedTitle;
        candidate.normalizedAuthor = normalizedAuthor;
        candidate.normalizedPublisher = normalizedPublisher;
        candidate.requestCount = 1;
        candidate.firstRequestedAt = firstRequestedAt;
        candidate.candidateStatus = "REQUESTED";
        return candidate;
    }
}
