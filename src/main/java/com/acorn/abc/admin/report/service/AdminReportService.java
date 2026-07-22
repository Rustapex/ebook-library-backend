package com.acorn.abc.admin.report.service;

import com.acorn.abc.admin.report.dto.AdminReportProcessRequest;
import com.acorn.abc.admin.report.dto.AdminReportProcessResponse;
import com.acorn.abc.admin.report.dto.AdminReportSummaryResponse;
import com.acorn.abc.admin.report.repository.AdminReportRepository;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.member.domain.Member;
import com.acorn.abc.member.repository.MemberRepository;
import com.acorn.abc.notification.domain.Notification;
import com.acorn.abc.notification.repository.NotificationRepository;
import com.acorn.abc.review.domain.BookReport;
import com.acorn.abc.review.domain.Review;
import com.acorn.abc.review.domain.ReviewReport;
import com.acorn.abc.review.domain.SanctionHistory;
import com.acorn.abc.review.repository.BookReportRepository;
import com.acorn.abc.review.repository.ReviewReportRepository;
import com.acorn.abc.review.repository.SanctionHistoryRepository;
import com.acorn.abc.statistic.domain.AdminAuditLogActionType;
import com.acorn.abc.statistic.domain.AdminAuditLogTargetType;
import com.acorn.abc.statistic.service.AdminAuditLogWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 관리자 신고 목록·처리(API-ADMIN-REPORT-001/002) 서비스(구현계획 2026-07-22 §2.4).
 * 처리 변경, 리뷰 숨김, 계정 정지, 신고 알림, 감사 로그는 하나의 transaction으로 처리한다 — 어느 하나가
 * 실패해도 나머지가 남지 않도록 별도 rollback 처리를 두지 않고 예외를 그대로 전파한다.
 */
@Service
@RequiredArgsConstructor
public class AdminReportService {

    private static final Set<String> TARGET_TYPES = Set.of("BOOK", "REVIEW");
    private static final Set<String> STATUS_VALUES = Set.of("WAITING", "PROCESSING", "DONE", "REJECTED");
    private static final Set<String> FINAL_STATUSES = Set.of("DONE", "REJECTED");
    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            "WAITING", Set.of("PROCESSING", "DONE", "REJECTED"),
            "PROCESSING", Set.of("DONE", "REJECTED")
    );

    private final AdminReportRepository adminReportRepository;
    private final BookReportRepository bookReportRepository;
    private final ReviewReportRepository reviewReportRepository;
    private final SanctionHistoryRepository sanctionHistoryRepository;
    private final MemberRepository memberRepository;
    private final NotificationRepository notificationRepository;
    private final AdminAuditLogWriter adminAuditLogWriter;

    @Transactional(readOnly = true)
    public PageResponse<AdminReportSummaryResponse> getAdminReports(String targetType, String status, int page, int size) {
        validatePaging(page, size);
        validateTargetType(targetType);
        validateStatus(status);

        Page<AdminReportRepository.AdminReportRow> result =
                adminReportRepository.search(targetType, status, PageRequest.of(page, size));
        List<AdminReportSummaryResponse> content = result.getContent().stream()
                .map(AdminReportSummaryResponse::from)
                .toList();

        return PageResponse.of(content, page, size, result.getTotalElements());
    }

    @Transactional
    public AdminReportProcessResponse processReport(String targetType, Long reportId, AdminReportProcessRequest request,
                                                       Long adminMemberId) {
        validateProcessRequest(targetType, request);

        if ("BOOK".equals(targetType)) {
            return processBookReport(reportId, request, adminMemberId);
        }
        return processReviewReport(reportId, request, adminMemberId);
    }

    private AdminReportProcessResponse processBookReport(Long reportId, AdminReportProcessRequest request, Long adminMemberId) {
        // PR64 코드리뷰 대응 §2.2 — 동일 신고 중복 처리를 막기 위해 신고 행을 먼저 잠근다.
        BookReport report = bookReportRepository.findByIdForUpdate(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_REPORT_NOT_FOUND));
        validateStatusTransition(report.getStatus(), request.status());

        String beforeStatus = report.getStatus();
        LocalDateTime now = LocalDateTime.now();
        report.process(request.status(), request.processResult(), now);

        notificationRepository.save(Notification.create(report.getReporter(), "REPORT",
                "신고가 처리되었습니다.", "BOOK_REPORT", reportId, now));

        adminAuditLogWriter.record(adminMemberId, AdminAuditLogActionType.REPORT_PROCESS, AdminAuditLogTargetType.BOOK_REPORT,
                reportId, Map.of("status", beforeStatus), Map.of("status", report.getStatus()));

        return new AdminReportProcessResponse(reportId, "BOOK", report.getStatus(), report.getProcessResult(),
                report.getProcessedAt(), null, null, null);
    }

    private AdminReportProcessResponse processReviewReport(Long reportId, AdminReportProcessRequest request, Long adminMemberId) {
        // PR64 코드리뷰 대응 §2.2 — 동일 신고 중복 처리를 막기 위해 신고 행을 먼저 잠근다.
        ReviewReport report = reviewReportRepository.findByIdForUpdate(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_REPORT_NOT_FOUND));
        validateStatusTransition(report.getStatus(), request.status());

        // 제재가 요청된 경우에만 리뷰 작성자 회원을 추가로 잠근다(§2.2) — 만료 배치의
        // findByIdForUpdate와 같은 행을 잠가 직렬화하고, 상태·리뷰·이력·알림·감사 로그를 만들기 전에
        // ADMIN/WITHDRAWN/DEACTIVATED 보호 대상이면 요청 전체를 403으로 거부한다.
        Member sanctionedMember = null;
        if (request.sanction() != null) {
            Long reviewAuthorId = report.getReview().getMember().getMemberId();
            sanctionedMember = memberRepository.findByIdForUpdate(reviewAuthorId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
            if (sanctionedMember.isProtectedFromAdminChanges()) {
                throw new BusinessException(ErrorCode.AUTH_ACCESS_DENIED);
            }
        }

        String beforeStatus = report.getStatus();
        LocalDateTime now = LocalDateTime.now();
        report.process(request.status(), request.processResult(), now);

        String reviewStatus = null;
        if (Boolean.TRUE.equals(request.hideReviewYn())) {
            Review review = report.getReview();
            review.hide();
            reviewStatus = review.getStatus();
        }

        Long sanctionHistoryId = null;
        String memberStatus = null;
        if (sanctionedMember != null) {
            // 이미 SANCTIONED인 회원의 추가 계정 정지도 허용한다 — 서로 다른 REVIEW 신고가 같은 회원을
            // 제재하면 이력은 신고 수만큼 남고 마지막 유효 제재가 currentSanction으로 연결된다(§2.2).
            SanctionHistory sanction = SanctionHistory.create(sanctionedMember, report, "ACCOUNT_SUSPENSION",
                    request.sanction().reason(), request.sanction().startedAt(), request.sanction().endedAt());
            sanctionHistoryRepository.save(sanction);
            sanctionedMember.applyAccountSuspension(sanction);
            sanctionHistoryId = sanction.getSanctionHistoryId();
            memberStatus = sanctionedMember.getStatus();
        }

        notificationRepository.save(Notification.create(report.getReporter(), "REPORT",
                "신고가 처리되었습니다.", "REVIEW_REPORT", reportId, now));

        adminAuditLogWriter.record(adminMemberId, AdminAuditLogActionType.REPORT_PROCESS, AdminAuditLogTargetType.REVIEW_REPORT,
                reportId, Map.of("status", beforeStatus),
                Map.of("status", report.getStatus(), "reviewHidden", String.valueOf(reviewStatus != null),
                        "sanctioned", String.valueOf(sanctionHistoryId != null)));

        return new AdminReportProcessResponse(reportId, "REVIEW", report.getStatus(), report.getProcessResult(),
                report.getProcessedAt(), reviewStatus, sanctionHistoryId, memberStatus);
    }

    private void validateProcessRequest(String targetType, AdminReportProcessRequest request) {
        validateTargetTypeRequired(targetType);
        if (request.status() == null || !STATUS_VALUES.contains(request.status())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        if (FINAL_STATUSES.contains(request.status())
                && (request.processResult() == null || request.processResult().isBlank())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        boolean isReviewDone = "REVIEW".equals(targetType) && "DONE".equals(request.status());

        if (Boolean.TRUE.equals(request.hideReviewYn()) && !isReviewDone) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        if (request.sanction() != null) {
            if (!isReviewDone) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR);
            }
            if (request.sanction().reason() == null || request.sanction().reason().isBlank()
                    || request.sanction().startedAt() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR);
            }
            if (request.sanction().endedAt() != null
                    && !request.sanction().endedAt().isAfter(request.sanction().startedAt())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR);
            }
        }
    }

    private void validateStatusTransition(String currentStatus, String newStatus) {
        Set<String> allowed = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowed.contains(newStatus)) {
            throw new BusinessException(ErrorCode.REPORT_INVALID_STATUS_TRANSITION);
        }
    }

    private void validateTargetTypeRequired(String targetType) {
        if (targetType == null || !TARGET_TYPES.contains(targetType)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private void validateTargetType(String targetType) {
        if (targetType != null && !TARGET_TYPES.contains(targetType)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private void validateStatus(String status) {
        if (status != null && !STATUS_VALUES.contains(status)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private void validatePaging(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }
}
