package com.acorn.abc.admin.request.service;

import com.acorn.abc.admin.request.dto.AdminBookRequestApplicantResponse;
import com.acorn.abc.admin.request.dto.AdminBookRequestCandidateResponse;
import com.acorn.abc.admin.request.dto.AdminBookRequestStatusUpdateRequest;
import com.acorn.abc.admin.request.dto.AdminBookRequestStatusUpdateResponse;
import com.acorn.abc.book.domain.Book;
import com.acorn.abc.book.repository.BookRepository;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.member.domain.Member;
import com.acorn.abc.notification.service.NotificationService;
import com.acorn.abc.request.domain.BookRequest;
import com.acorn.abc.request.domain.BookRequestCandidate;
import com.acorn.abc.request.repository.BookRequestCandidateRepository;
import com.acorn.abc.request.repository.BookRequestRepository;
import com.acorn.abc.statistic.domain.AdminAuditLogActionType;
import com.acorn.abc.statistic.domain.AdminAuditLogTargetType;
import com.acorn.abc.statistic.service.AdminAuditLogWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminBookRequestService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;
    private static final Set<String> VALID_CANDIDATE_STATUSES = Set.of("REQUESTED", "IN_REVIEW", "APPROVED", "REJECTED");
    private static final Set<String> PATCH_ALLOWED_STATUSES = Set.of("IN_REVIEW", "APPROVED", "REJECTED");

    private final BookRequestCandidateRepository candidateRepository;
    private final BookRequestRepository bookRequestRepository;
    private final BookRepository bookRepository;
    private final NotificationService notificationService;
    private final AdminAuditLogWriter adminAuditLogWriter;

    @Transactional(readOnly = true)
    public PageResponse<AdminBookRequestCandidateResponse> getCandidates(String status, String q, Integer page, Integer size) {
        String normalizedStatus = normalizeStatusFilter(status);
        String normalizedQ = normalizeQ(q);
        Pageable pageable = PageRequest.of(normalizePage(page), normalizeSize(size));
        Page<BookRequestCandidate> candidates = candidateRepository.search(normalizedStatus, normalizedQ, pageable);
        List<AdminBookRequestCandidateResponse> content = candidates.getContent().stream()
                .map(this::toCandidateResponse)
                .toList();
        return PageResponse.of(content, candidates.getNumber(), candidates.getSize(), candidates.getTotalElements());
    }

    public AdminBookRequestStatusUpdateResponse updateStatus(Long adminMemberId, Long candidateId,
                                                               AdminBookRequestStatusUpdateRequest request) {
        String status = normalizeRequiredStatus(request.status());
        validateFields(status, request.approvedBookId(), request.rejectReason());

        if ("IN_REVIEW".equals(status)) {
            return moveToInReview(adminMemberId, candidateId);
        }
        return finalizeCandidate(adminMemberId, candidateId, status, request.approvedBookId(), request.rejectReason());
    }

    private AdminBookRequestStatusUpdateResponse moveToInReview(Long adminMemberId, Long candidateId) {
        int updated = candidateRepository.moveToInReviewIfRequested(candidateId);
        ensureUpdated(candidateId, updated);

        long affected = bookRequestRepository.countByRequestCandidate_RequestCandidateIdAndRequestStatus(candidateId, "REQUESTED");

        Map<String, Object> afterSummary = new LinkedHashMap<>();
        afterSummary.put("status", "IN_REVIEW");
        afterSummary.put("affectedRequestCount", affected);
        adminAuditLogWriter.record(adminMemberId, AdminAuditLogActionType.BOOK_REQUEST_STATUS_UPDATE,
                AdminAuditLogTargetType.BOOK_REQUEST_CANDIDATE, candidateId, null, afterSummary);

        return new AdminBookRequestStatusUpdateResponse(candidateId, "IN_REVIEW", (int) affected, null, null);
    }

    private AdminBookRequestStatusUpdateResponse finalizeCandidate(Long adminMemberId, Long candidateId, String status,
                                                                     Long approvedBookId, String rejectReason) {
        Book approvedBook = null;
        if ("APPROVED".equals(status)) {
            approvedBook = bookRepository.findById(approvedBookId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND));
        }

        // candidate 조건부 UPDATE를 먼저 실행해 해당 후보 row의 InnoDB 잠금을 우리 트랜잭션이 보유한 뒤에
        // 알림 대상(이번 회차 REQUESTED 회원)을 조회한다. 잠금 이전에 조회하면, 동시에 들어온 신규 신청이
        // candidate/book_request에 반영된 뒤 우리가 그 신규 row를 놓친 채로 나머지를 승인 처리하면서
        // 새로 들어온 신청자에게는 알림이 누락되는 경쟁 상태가 생길 수 있다.
        int candidateUpdated = candidateRepository.finalizeIfEligible(candidateId, status);
        ensureUpdated(candidateId, candidateUpdated);

        List<BookRequest> targetRequests = bookRequestRepository.findRequestedByCandidateId(candidateId);

        int affectedRequestCount = bookRequestRepository.updateStatusForRequestedByCandidateId(
                candidateId, status, approvedBook, rejectReason, LocalDateTime.now());
        if (affectedRequestCount == 0) {
            // candidate 조건부 UPDATE는 성공했는데 대상 row가 없는 것은 정상 흐름상 불가능한 상태 불일치 —
            // 예외를 던져 같은 트랜잭션 안에서 candidate 변경까지 롤백시킨다.
            throw new BusinessException(ErrorCode.BOOK_REQUEST_INVALID_STATUS);
        }

        notifyAffectedMembers(targetRequests, status, candidateId);

        Map<String, Object> afterSummary = new LinkedHashMap<>();
        afterSummary.put("status", status);
        afterSummary.put("affectedRequestCount", affectedRequestCount);
        afterSummary.put("approvedBookId", approvedBookId);
        afterSummary.put("rejectReason", rejectReason);
        adminAuditLogWriter.record(adminMemberId, AdminAuditLogActionType.BOOK_REQUEST_STATUS_UPDATE,
                AdminAuditLogTargetType.BOOK_REQUEST_CANDIDATE, candidateId, null, afterSummary);

        return new AdminBookRequestStatusUpdateResponse(candidateId, status, affectedRequestCount, approvedBookId, rejectReason);
    }

    private void notifyAffectedMembers(List<BookRequest> targetRequests, String status, Long candidateId) {
        if (targetRequests.isEmpty()) {
            return;
        }
        String displayTitle = targetRequests.get(0).getRequestTitle();
        String title = "APPROVED".equals(status)
                ? "'" + displayTitle + "' 희망도서 신청이 승인되었습니다."
                : "'" + displayTitle + "' 희망도서 신청이 반려되었습니다.";
        List<Member> members = targetRequests.stream().map(BookRequest::getMember).toList();
        notificationService.notifyMembers(members, "BOOK_REQUEST", title, "BOOK_REQUEST_CANDIDATE", candidateId);
    }

    private void ensureUpdated(Long candidateId, int updatedRows) {
        if (updatedRows == 0) {
            if (!candidateRepository.existsById(candidateId)) {
                throw new BusinessException(ErrorCode.BOOK_REQUEST_CANDIDATE_NOT_FOUND);
            }
            throw new BusinessException(ErrorCode.BOOK_REQUEST_INVALID_STATUS);
        }
    }

    private AdminBookRequestCandidateResponse toCandidateResponse(BookRequestCandidate candidate) {
        Long candidateId = candidate.getRequestCandidateId();
        List<AdminBookRequestApplicantResponse> applicants = bookRequestRepository.findApplicantsByCandidateId(candidateId);

        BookRequest representative = bookRequestRepository
                .findFirstByRequestCandidate_RequestCandidateIdOrderByCreatedAtAsc(candidateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_REQUEST_CANDIDATE_NOT_FOUND));

        LocalDateTime latestRequestedAt = applicants.stream()
                .map(AdminBookRequestApplicantResponse::requestedAt)
                .max(LocalDateTime::compareTo)
                .orElse(candidate.getFirstRequestedAt());

        String status = candidate.getCandidateStatus();
        Long approvedBookId = null;
        String rejectReason = null;
        if ("APPROVED".equals(status) || "REJECTED".equals(status)) {
            Optional<BookRequest> latest = bookRequestRepository
                    .findFirstByRequestCandidate_RequestCandidateIdAndRequestStatusOrderByProcessedAtDescRequestIdDesc(candidateId, status);
            if (latest.isPresent()) {
                BookRequest lr = latest.get();
                approvedBookId = "APPROVED".equals(status) && lr.getApprovedBook() != null ? lr.getApprovedBook().getBookId() : null;
                rejectReason = "REJECTED".equals(status) ? lr.getRejectReason() : null;
            }
        }

        return new AdminBookRequestCandidateResponse(
                candidateId, representative.getRequestTitle(), representative.getRequestAuthor(), representative.getRequestPublisher(),
                status, candidate.getRequestCount(), candidate.getFirstRequestedAt(), latestRequestedAt,
                applicants, rejectReason, approvedBookId);
    }

    private void validateFields(String status, Long approvedBookId, String rejectReason) {
        boolean hasApprovedBookId = approvedBookId != null;
        boolean hasRejectReason = rejectReason != null && !rejectReason.isBlank();

        switch (status) {
            case "IN_REVIEW" -> {
                if (hasApprovedBookId || hasRejectReason) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR);
                }
            }
            case "APPROVED" -> {
                if (!hasApprovedBookId || approvedBookId <= 0) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR);
                }
                if (hasRejectReason) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR);
                }
            }
            case "REJECTED" -> {
                if (!hasRejectReason) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR);
                }
                if (hasApprovedBookId) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR);
                }
            }
            default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private String normalizeRequiredStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        String value = status.trim().toUpperCase(Locale.ROOT);
        if (!PATCH_ALLOWED_STATUSES.contains(value)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        return value;
    }

    private String normalizeStatusFilter(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String value = status.trim().toUpperCase(Locale.ROOT);
        if (!VALID_CANDIDATE_STATUSES.contains(value)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        return value;
    }

    private String normalizeQ(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        return q.trim().toLowerCase(Locale.ROOT);
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
