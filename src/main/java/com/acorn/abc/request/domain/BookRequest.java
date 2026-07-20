package com.acorn.abc.request.domain;

import com.acorn.abc.book.domain.Book;
import com.acorn.abc.member.domain.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "book_request")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_candidate_id")
    private BookRequestCandidate requestCandidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_book_id")
    private Book approvedBook;

    @Column(name = "request_title", nullable = false, length = 255)
    private String requestTitle;

    @Column(name = "request_author", nullable = false, length = 100)
    private String requestAuthor;

    @Column(name = "request_publisher", nullable = false, length = 100)
    private String requestPublisher;

    @Column(name = "normalized_title", nullable = false, length = 255)
    private String normalizedTitle;

    @Column(name = "normalized_author", nullable = false, length = 100)
    private String normalizedAuthor;

    @Column(name = "normalized_publisher", nullable = false, length = 100)
    private String normalizedPublisher;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "request_status", nullable = false, length = 20)
    private String requestStatus;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static BookRequest create(BookRequestCandidate requestCandidate, Member member,
                                      String requestTitle, String requestAuthor, String requestPublisher,
                                      String normalizedTitle, String normalizedAuthor, String normalizedPublisher,
                                      String reason, LocalDateTime createdAt) {
        BookRequest request = new BookRequest();
        request.requestCandidate = requestCandidate;
        request.member = member;
        request.requestTitle = requestTitle;
        request.requestAuthor = requestAuthor;
        request.requestPublisher = requestPublisher;
        request.normalizedTitle = normalizedTitle;
        request.normalizedAuthor = normalizedAuthor;
        request.normalizedPublisher = normalizedPublisher;
        request.reason = reason;
        request.requestStatus = "REQUESTED";
        request.createdAt = createdAt;
        return request;
    }
}
