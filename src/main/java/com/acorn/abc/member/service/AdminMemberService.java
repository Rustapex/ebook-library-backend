package com.acorn.abc.member.service;

import com.acorn.abc.admin.member.dto.AdminMemberDetailResponse;
import com.acorn.abc.admin.member.repository.AdminMemberReportRepository;
import com.acorn.abc.admin.member.dto.AdminMemberPaymentTabItemResponse;
import com.acorn.abc.admin.member.dto.AdminMemberProfileResponse;
import com.acorn.abc.admin.member.dto.AdminMemberRentalTabItemResponse;
import com.acorn.abc.admin.member.dto.AdminMemberReportTabItemResponse;
import com.acorn.abc.admin.member.dto.AdminMemberSanctionSummaryResponse;
import com.acorn.abc.admin.member.dto.AdminMemberSanctionTabItemResponse;
import com.acorn.abc.admin.member.dto.AdminMemberStatusChangeRequest;
import com.acorn.abc.admin.member.dto.AdminMemberStatusChangeResponse;
import com.acorn.abc.admin.member.dto.AdminMemberSummaryResponse;
import com.acorn.abc.admin.member.dto.AdminMemberTabResponse;
import com.acorn.abc.admin.member.dto.AdminMemberUsageSummaryResponse;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.member.domain.Member;
import com.acorn.abc.member.domain.PointHistory;
import com.acorn.abc.member.dto.PointHistoryResponse;
import com.acorn.abc.member.repository.MemberRepository;
import com.acorn.abc.member.repository.PointHistoryRepository;
import com.acorn.abc.payment.domain.Payment;
import com.acorn.abc.payment.repository.PaymentRepository;
import com.acorn.abc.rental.domain.BookRental;
import com.acorn.abc.rental.repository.BookRentalRepository;
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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 관리자 회원 API(API-ADMIN-MEMBER-001~003) 서비스.
 * 포인트 조정(004)은 별도 멱등성 요구사항 때문에 AdminPointAdjustService로 분리되어 있다(구현계획 §2.2).
 */
@Service
@RequiredArgsConstructor
public class AdminMemberService {

    private static final Set<String> STATUS_VALUES = Set.of("JOINED", "SANCTIONED", "WITHDRAWN", "DEACTIVATED");
    private static final Set<String> ROLE_VALUES = Set.of("USER", "ADMIN");

    private final MemberRepository memberRepository;
    private final SanctionHistoryRepository sanctionHistoryRepository;
    private final BookRentalRepository bookRentalRepository;
    private final PaymentRepository paymentRepository;
    private final BookReportRepository bookReportRepository;
    private final ReviewReportRepository reviewReportRepository;
    private final AdminMemberReportRepository adminMemberReportRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final AdminAuditLogWriter adminAuditLogWriter;

    @Transactional(readOnly = true)
    public PageResponse<AdminMemberSummaryResponse> getAdminMembers(String q, String status, String role,
                                                                      Long gradeId, int page, int size) {
        validatePaging(page, size);
        validateStatus(status);
        validateRole(role);

        String likeQ = (q == null || q.isBlank()) ? null : "%" + q.trim().toLowerCase() + "%";
        Pageable pageable = PageRequest.of(page, size);
        Page<Member> result = memberRepository.searchForAdmin(likeQ, status, role, gradeId, pageable);

        List<Long> memberIds = result.getContent().stream().map(Member::getMemberId).toList();
        Map<Long, List<AdminMemberSanctionSummaryResponse>> sanctionsByMember = memberIds.isEmpty()
                ? Map.of()
                : sanctionHistoryRepository.findActiveSanctionSummariesByMemberIds(memberIds, LocalDateTime.now()).stream()
                    .collect(Collectors.groupingBy(
                            SanctionHistoryRepository.ActiveSanctionSummaryView::getMemberId,
                            Collectors.mapping(v -> new AdminMemberSanctionSummaryResponse(v.getSanctionType(), v.getEndedAt()),
                                    Collectors.toList())));

        List<AdminMemberSummaryResponse> content = result.getContent().stream()
                .map(m -> new AdminMemberSummaryResponse(
                        m.getMemberId(), m.getLoginId(), m.getName(), maskEmail(m.getEmail()), maskPhone(m.getPhone()),
                        m.getRole(), m.getStatus(), m.getGrade().getGradeName(),
                        sanctionsByMember.getOrDefault(m.getMemberId(), List.of()), m.getCreatedAt()))
                .toList();

        return PageResponse.of(content, page, size, result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public AdminMemberDetailResponse getAdminMemberDetail(Long memberId) {
        Member member = getMemberOrThrow(memberId);

        AdminMemberProfileResponse profile = new AdminMemberProfileResponse(
                member.getMemberId(), member.getLoginId(), member.getName(), member.getEmail(), member.getPhone(),
                member.getBirthDate(), member.getGender(), member.getRole(), member.getStatus(),
                member.getGrade().getGradeName(), member.getCreatedAt());

        long rentalCount = bookRentalRepository.countByMember_MemberId(memberId);
        long paymentCount = paymentRepository.countByMember_MemberId(memberId);
        long reportCount = bookReportRepository.countByReporter_MemberId(memberId)
                + reviewReportRepository.countByReporter_MemberId(memberId);
        List<AdminMemberSanctionSummaryResponse> activeSanctions = sanctionHistoryRepository
                .findActiveSanctionSummariesByMemberIds(List.of(memberId), LocalDateTime.now()).stream()
                .map(v -> new AdminMemberSanctionSummaryResponse(v.getSanctionType(), v.getEndedAt()))
                .toList();

        AdminMemberUsageSummaryResponse usageSummary = new AdminMemberUsageSummaryResponse(
                member.getPointBalance(), rentalCount, paymentCount, reportCount, activeSanctions);

        return new AdminMemberDetailResponse(memberId, profile, usageSummary);
    }

    @Transactional(readOnly = true)
    public AdminMemberTabResponse<AdminMemberRentalTabItemResponse> getAdminMemberRentals(Long memberId, int page, int size) {
        ensureMemberExists(memberId);
        validatePaging(page, size);
        Page<BookRental> result = bookRentalRepository.findByMemberIdForAdmin(memberId, PageRequest.of(page, size));
        List<AdminMemberRentalTabItemResponse> content = result.getContent().stream()
                .map(r -> new AdminMemberRentalTabItemResponse(r.getRentalId(), r.getBook().getBookId(), r.getBook().getTitle(),
                        r.getRentalStatus(), r.getRentalStartAt(), r.getRentalEndAt(), r.getCreatedAt()))
                .toList();
        return new AdminMemberTabResponse<>(memberId, PageResponse.of(content, page, size, result.getTotalElements()));
    }

    @Transactional(readOnly = true)
    public AdminMemberTabResponse<AdminMemberPaymentTabItemResponse> getAdminMemberPayments(Long memberId, int page, int size) {
        ensureMemberExists(memberId);
        validatePaging(page, size);
        Page<Payment> result = paymentRepository.findByMemberIdForAdmin(memberId, PageRequest.of(page, size));
        List<AdminMemberPaymentTabItemResponse> content = result.getContent().stream()
                .map(p -> new AdminMemberPaymentTabItemResponse(p.getPaymentId(), p.getRental().getRentalId(),
                        p.getRental().getBook().getBookId(), p.getRental().getBook().getTitle(), p.getPaymentType(),
                        p.getAmount(), p.getPaymentMethod(), p.getPaymentStatus(), p.getPaidAt()))
                .toList();
        return new AdminMemberTabResponse<>(memberId, PageResponse.of(content, page, size, result.getTotalElements()));
    }

    @Transactional(readOnly = true)
    public AdminMemberTabResponse<AdminMemberReportTabItemResponse> getAdminMemberReports(Long memberId, int page, int size) {
        ensureMemberExists(memberId);
        validatePaging(page, size);

        // PR51 코드리뷰 대응 §2.3-3 — book_report/review_report 전체를 메모리에 적재해 합치지 않고,
        // UNION ALL 쿼리 자체에 정렬·페이지(LIMIT/OFFSET)를 적용한다.
        Page<AdminMemberReportRepository.AdminMemberReportRow> result =
                adminMemberReportRepository.findByReporterId(memberId, PageRequest.of(page, size));
        List<AdminMemberReportTabItemResponse> content = result.getContent().stream()
                .map(row -> new AdminMemberReportTabItemResponse(row.getReportId(), row.getReportSourceType(),
                        row.getReportType(), row.getStatus(), row.getProcessedAt(), row.getCreatedAt()))
                .toList();

        return new AdminMemberTabResponse<>(memberId, PageResponse.of(content, page, size, result.getTotalElements()));
    }

    @Transactional(readOnly = true)
    public AdminMemberTabResponse<PointHistoryResponse> getAdminMemberPoints(Long memberId, int page, int size) {
        ensureMemberExists(memberId);
        validatePaging(page, size);
        Page<PointHistory> result = pointHistoryRepository.findByMemberIdForAdmin(memberId, PageRequest.of(page, size));
        List<PointHistoryResponse> content = result.getContent().stream()
                .map(p -> new PointHistoryResponse(p.getPointHistoryId(), p.getPointType(), p.getPointAmount(),
                        p.getDescription(), p.getCreatedAt()))
                .toList();
        return new AdminMemberTabResponse<>(memberId, PageResponse.of(content, page, size, result.getTotalElements()));
    }

    @Transactional(readOnly = true)
    public AdminMemberTabResponse<AdminMemberSanctionTabItemResponse> getAdminMemberSanctions(Long memberId, int page, int size) {
        ensureMemberExists(memberId);
        validatePaging(page, size);
        Page<SanctionHistory> result = sanctionHistoryRepository.findByMemberIdForAdmin(memberId, PageRequest.of(page, size));
        List<AdminMemberSanctionTabItemResponse> content = result.getContent().stream()
                .map(s -> new AdminMemberSanctionTabItemResponse(s.getSanctionHistoryId(), s.getSanctionType(), s.getReason(),
                        s.getStartedAt(), s.getEndedAt(), s.getCreatedAt()))
                .toList();
        return new AdminMemberTabResponse<>(memberId, PageResponse.of(content, page, size, result.getTotalElements()));
    }

    @Transactional
    public AdminMemberStatusChangeResponse changeMemberStatus(Long memberId, AdminMemberStatusChangeRequest request,
                                                                Long adminMemberId) {
        validateStatusChangeRequest(request);

        Member member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (member.isProtectedFromAdminChanges()) {
            throw new BusinessException(ErrorCode.AUTH_ACCESS_DENIED);
        }

        String beforeStatus = member.getStatus();
        LocalDateTime startedAt = request.startedAt() != null ? request.startedAt() : LocalDateTime.now();

        if (request.sanctionType() != null) {
            SanctionHistory sanction = SanctionHistory.create(member, null, request.sanctionType(), request.reason(),
                    startedAt, request.endedAt());
            sanctionHistoryRepository.save(sanction);
            if ("ACCOUNT_SUSPENSION".equals(request.sanctionType())) {
                member.applyAccountSuspension(sanction);
            }
            // RENTAL_BAN/REVIEW_BAN: SANCTION_HISTORY만 생성하고 회원 상태는 바꾸지 않는다(§1.4-2).
        } else {
            member.changeStatusDirectly(request.status());
        }

        adminAuditLogWriter.record(adminMemberId, AdminAuditLogActionType.MEMBER_UPDATE, AdminAuditLogTargetType.MEMBER,
                memberId, Map.of("status", beforeStatus),
                Map.of("status", member.getStatus(), "sanctionType", request.sanctionType() == null ? "" : request.sanctionType(),
                        "reason", request.reason()));

        AdminMemberSanctionSummaryResponse currentSanction = member.getCurrentSanction() == null ? null
                : new AdminMemberSanctionSummaryResponse(member.getCurrentSanction().getSanctionType(),
                        member.getCurrentSanction().getEndedAt());
        return new AdminMemberStatusChangeResponse(memberId, member.getStatus(), currentSanction);
    }

    /**
     * 계정 정지 만료 배치(구현계획 §1.5) 후보 1건 처리. AccountSuspensionExpiryBatchService가
     * 후보마다 별도 호출한다 — 같은 클래스 내 자기호출로는 @Transactional이 적용되지 않으므로
     * 배치 오케스트레이션은 다른 빈으로 분리되어 있다.
     */
    @Transactional
    public boolean restoreAccountSuspensionIfEligible(Long sanctionHistoryId, LocalDateTime now) {
        SanctionHistory sanction = sanctionHistoryRepository.findByIdForUpdate(sanctionHistoryId).orElse(null);
        if (sanction == null) {
            return false;
        }
        Member member = memberRepository.findByIdForUpdate(sanction.getMember().getMemberId()).orElse(null);
        if (member == null || member.isAdmin()) {
            return false;
        }
        if (!"SANCTIONED".equals(member.getStatus())) {
            return false;
        }
        Long currentSanctionId = member.getCurrentSanction() == null ? null : member.getCurrentSanction().getSanctionHistoryId();
        if (currentSanctionId == null || !currentSanctionId.equals(sanctionHistoryId)) {
            return false;
        }
        if (sanction.getEndedAt() == null || !sanction.getEndedAt().isBefore(now)) {
            return false;
        }

        List<SanctionHistory> stillActive = sanctionHistoryRepository.findActiveAccountSuspensionsByMember(member.getMemberId(), now);
        if (!stillActive.isEmpty()) {
            // 다른 활성 계정 정지로 원인만 재지정 — 이력·감사 로그를 남기지 않는다(§1.5-6).
            member.reassignCurrentSanction(stillActive.get(0));
            return false;
        }

        member.restoreFromSuspension();
        adminAuditLogWriter.record(null, AdminAuditLogActionType.MEMBER_UPDATE, AdminAuditLogTargetType.MEMBER,
                member.getMemberId(), Map.of("status", "SANCTIONED", "currentSanctionId", sanctionHistoryId),
                Map.of("status", "JOINED"));
        return true;
    }

    private void validateStatusChangeRequest(AdminMemberStatusChangeRequest request) {
        String sanctionType = request.sanctionType();
        String status = request.status();
        if ("ACCOUNT_SUSPENSION".equals(sanctionType) && !"SANCTIONED".equals(status)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        if (("RENTAL_BAN".equals(sanctionType) || "REVIEW_BAN".equals(sanctionType)) && !"JOINED".equals(status)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        if (sanctionType == null && "SANCTIONED".equals(status)) {
            // SANCTIONED는 ACCOUNT_SUSPENSION 없이 성립할 수 없다 — currentSanctionId 원인 연결이 끊긴 상태를 방지한다.
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        if (sanctionType != null && request.startedAt() != null && request.endedAt() != null
                && !request.endedAt().isAfter(request.startedAt())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private Member getMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private void ensureMemberExists(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
    }

    private void validatePaging(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private void validateStatus(String status) {
        if (status != null && !STATUS_VALUES.contains(status)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private void validateRole(String role) {
        if (role != null && !ROLE_VALUES.contains(role)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    // 목록 응답 개인정보 최소화(§1.2) — 이메일 로컬파트 앞 1~2자만 노출.
    private String maskEmail(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        String local = email.substring(0, at);
        String domain = email.substring(at);
        String visible = local.length() <= 2 ? local.substring(0, 1) : local.substring(0, 2);
        return visible + "***" + domain;
    }

    // 목록 응답 개인정보 최소화(§1.2) — 앞자리 일부/뒤 4자리만 노출.
    private String maskPhone(String phone) {
        if (phone == null) {
            return null;
        }
        int len = phone.length();
        if (len <= 4) {
            return "*".repeat(len);
        }
        int headLen = Math.min(3, len - 4);
        String head = phone.substring(0, headLen);
        String tail = phone.substring(len - 4);
        return head + "*".repeat(len - headLen - 4) + tail;
    }
}
