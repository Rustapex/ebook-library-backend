package com.acorn.abc.request.service;

import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.member.domain.Member;
import com.acorn.abc.member.repository.MemberRepository;
import com.acorn.abc.request.domain.BookRequest;
import com.acorn.abc.request.domain.BookRequestCandidate;
import com.acorn.abc.request.dto.BookRequestCreateRequest;
import com.acorn.abc.request.dto.BookRequestCreateResponse;
import com.acorn.abc.request.dto.BookRequestHistoryItemResponse;
import com.acorn.abc.request.repository.BookRequestCandidateRepository;
import com.acorn.abc.request.repository.BookRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class BookRequestService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;
    private static final Set<String> VALID_STATUSES = Set.of("REQUESTED", "IN_REVIEW", "APPROVED", "REJECTED");
    private static final String UNIQUE_MEMBER_NORMALIZED_CONSTRAINT = "uk_book_request_member_normalized";

    private final MemberRepository memberRepository;
    private final BookRequestRepository bookRequestRepository;
    private final BookRequestCandidateRepository bookRequestCandidateRepository;

    public BookRequestCreateResponse createBookRequest(Long memberId, BookRequestCreateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        String normalizedTitle = normalize(request.title());
        String normalizedAuthor = normalize(request.author());
        String normalizedPublisher = normalize(request.publisher());
        LocalDateTime now = LocalDateTime.now();

        // 빠른 실패: 같은 회원의 동일 정규화 조합 재신청은 여기서 대부분 걸러진다.
        // UNIQUE 제약(uk_book_request_member_normalized)이 동시성 상황의 최종 방어선이다.
        if (bookRequestRepository.existsByMember_MemberIdAndNormalizedTitleAndNormalizedAuthorAndNormalizedPublisher(
                memberId, normalizedTitle, normalizedAuthor, normalizedPublisher)) {
            throw new BusinessException(ErrorCode.BOOK_REQUEST_ALREADY_EXISTS);
        }

        bookRequestCandidateRepository.upsertCandidate(normalizedTitle, normalizedAuthor, normalizedPublisher, now);
        Long candidateId = bookRequestCandidateRepository.lastInsertId();
        BookRequestCandidate candidate = bookRequestCandidateRepository.getReferenceById(candidateId);

        BookRequest bookRequest = BookRequest.create(candidate, member,
                request.title(), request.author(), request.publisher(),
                normalizedTitle, normalizedAuthor, normalizedPublisher,
                request.reason(), now);

        try {
            bookRequestRepository.saveAndFlush(bookRequest);
        } catch (DataIntegrityViolationException e) {
            if (isUniqueMemberNormalizedViolation(e)) {
                throw new BusinessException(ErrorCode.BOOK_REQUEST_ALREADY_EXISTS);
            }
            throw e;
        }

        int requestCount = bookRequestCandidateRepository.findById(candidateId)
                .map(BookRequestCandidate::getRequestCount)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_REQUEST_CANDIDATE_NOT_FOUND));

        return new BookRequestCreateResponse(bookRequest.getRequestId(), candidateId, bookRequest.getRequestStatus(), requestCount);
    }

    @Transactional(readOnly = true)
    public PageResponse<BookRequestHistoryItemResponse> getMyBookRequests(Long memberId, String status, Integer page, Integer size) {
        String normalizedStatus = normalizeStatus(status);
        Pageable pageable = PageRequest.of(normalizePage(page), normalizeSize(size));
        Page<BookRequest> requests = bookRequestRepository.findMyRequests(memberId, normalizedStatus, pageable);
        List<BookRequestHistoryItemResponse> content = requests.getContent().stream()
                .map(this::toHistoryItem)
                .toList();
        return PageResponse.of(content, requests.getNumber(), requests.getSize(), requests.getTotalElements());
    }

    private BookRequestHistoryItemResponse toHistoryItem(BookRequest br) {
        String computedStatus = computeStatus(br);
        Long approvedBookId = "APPROVED".equals(br.getRequestStatus()) && br.getApprovedBook() != null
                ? br.getApprovedBook().getBookId() : null;
        String rejectReason = "REJECTED".equals(br.getRequestStatus()) ? br.getRejectReason() : null;
        return new BookRequestHistoryItemResponse(
                br.getRequestId(), br.getRequestTitle(), br.getRequestAuthor(), br.getRequestPublisher(),
                br.getReason(), br.getCreatedAt(), computedStatus, br.getRequestCandidate().getRequestCount(),
                approvedBookId, rejectReason);
    }

    private String computeStatus(BookRequest br) {
        if ("REQUESTED".equals(br.getRequestStatus()) && "IN_REVIEW".equals(br.getRequestCandidate().getCandidateStatus())) {
            return "IN_REVIEW";
        }
        return br.getRequestStatus();
    }

    private boolean isUniqueMemberNormalizedViolation(DataIntegrityViolationException e) {
        Throwable cause = e.getMostSpecificCause();
        String message = cause == null ? null : cause.getMessage();
        return message != null && message.contains(UNIQUE_MEMBER_NORMALIZED_CONSTRAINT);
    }

    private String normalize(String value) {
        String nfkc = Normalizer.normalize(value, Normalizer.Form.NFKC);
        String collapsed = nfkc.trim().replaceAll("\\s+", " ");
        return collapsed.toLowerCase(Locale.ROOT);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String value = status.trim().toUpperCase(Locale.ROOT);
        if (!VALID_STATUSES.contains(value)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        return value;
    }

    private int normalizePage(Integer page) {
        return page == null || page < 0 ? DEFAULT_PAGE : page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
