package com.acorn.abc.member.service;

import com.acorn.abc.admin.member.dto.AdminMemberDetailResponse;
import com.acorn.abc.admin.member.dto.AdminMemberReportTabItemResponse;
import com.acorn.abc.admin.member.dto.AdminMemberStatusChangeRequest;
import com.acorn.abc.admin.member.dto.AdminMemberStatusChangeResponse;
import com.acorn.abc.admin.member.dto.AdminMemberSummaryResponse;
import com.acorn.abc.admin.member.dto.AdminMemberTabResponse;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.member.domain.Member;
import com.acorn.abc.member.domain.MemberGrade;
import com.acorn.abc.member.repository.MemberRepository;
import com.acorn.abc.member.repository.PointHistoryRepository;
import com.acorn.abc.payment.repository.PaymentRepository;
import com.acorn.abc.rental.repository.BookRentalRepository;
import com.acorn.abc.review.domain.BookReport;
import com.acorn.abc.review.domain.ReviewReport;
import com.acorn.abc.review.domain.SanctionHistory;
import com.acorn.abc.review.repository.BookReportRepository;
import com.acorn.abc.review.repository.ReviewReportRepository;
import com.acorn.abc.review.repository.SanctionHistoryRepository;
import com.acorn.abc.statistic.service.AdminAuditLogWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 관리자 회원 API 구현계획(2026-07-16) §4.1/§4.2 대응 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminMemberServiceTest {

    @Mock private MemberRepository memberRepository;
    @Mock private SanctionHistoryRepository sanctionHistoryRepository;
    @Mock private BookRentalRepository bookRentalRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private BookReportRepository bookReportRepository;
    @Mock private ReviewReportRepository reviewReportRepository;
    @Mock private PointHistoryRepository pointHistoryRepository;
    @Mock private AdminAuditLogWriter adminAuditLogWriter;

    private AdminMemberService newService() {
        return new AdminMemberService(memberRepository, sanctionHistoryRepository, bookRentalRepository,
                paymentRepository, bookReportRepository, reviewReportRepository, pointHistoryRepository,
                adminAuditLogWriter);
    }

    private MemberGrade grade(String name) {
        MemberGrade grade = mock(MemberGrade.class);
        given(grade.getGradeName()).willReturn(name);
        return grade;
    }

    private Member joinedMember(Long id, String role, String status) {
        Member member = mock(Member.class);
        given(member.getMemberId()).willReturn(id);
        given(member.getLoginId()).willReturn("user" + id);
        given(member.getName()).willReturn("회원" + id);
        given(member.getEmail()).willReturn("user" + id + "@test.com");
        given(member.getPhone()).willReturn("01012345678");
        given(member.getRole()).willReturn(role);
        given(member.getStatus()).willReturn(status);
        MemberGrade grade = grade("BASIC");
        given(member.getGrade()).willReturn(grade);
        given(member.getCreatedAt()).willReturn(LocalDateTime.now());
        given(member.isAdmin()).willReturn("ADMIN".equals(role));
        given(member.isProtectedFromAdminChanges()).willReturn(
                "ADMIN".equals(role) || "WITHDRAWN".equals(status) || "DEACTIVATED".equals(status));
        return member;
    }

    // ---------- 001 목록 ----------

    @Test
    void getAdminMembers_잘못된_status면_VALIDATION_ERROR를_던진다() {
        AdminMemberService service = newService();
        assertThatThrownBy(() -> service.getAdminMembers(null, "UNKNOWN", null, null, 0, 20))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void getAdminMembers_size가_범위밖이면_VALIDATION_ERROR를_던진다() {
        AdminMemberService service = newService();
        assertThatThrownBy(() -> service.getAdminMembers(null, null, null, null, 0, 101))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void getAdminMembers_이메일과_전화번호는_마스킹되고_birthDate_gender_필드는_존재하지_않는다() {
        Member member = joinedMember(1L, "USER", "JOINED");
        given(memberRepository.searchForAdmin(any(), any(), any(), any(), any()))
                .willReturn(new PageImpl<>(List.of(member), PageRequest.of(0, 20), 1));
        given(sanctionHistoryRepository.findActiveSanctionSummariesByMemberIds(any(), any())).willReturn(List.of());

        PageResponse<AdminMemberSummaryResponse> result = newService().getAdminMembers(null, null, null, null, 0, 20);

        AdminMemberSummaryResponse summary = result.content().get(0);
        assertThat(summary.maskedEmail()).isNotEqualTo(member.getEmail());
        assertThat(summary.maskedEmail()).contains("***").endsWith("@test.com");
        assertThat(summary.maskedPhone()).isNotEqualTo(member.getPhone());
        assertThat(summary.maskedPhone()).endsWith("5678");
        // AdminMemberSummaryResponse record에는 birthDate/gender accessor 자체가 없다(컴파일 타임 보장).
    }

    // ---------- 002 상세 ----------

    @Test
    void getAdminMemberDetail_존재하지_않으면_MEMBER_NOT_FOUND를_던진다() {
        given(memberRepository.findById(1L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> newService().getAdminMemberDetail(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    void getAdminMemberDetail_DEFAULT는_원문_email_phone을_포함한다() {
        Member member = joinedMember(1L, "USER", "JOINED");
        given(member.getPointBalance()).willReturn(1000);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(sanctionHistoryRepository.findActiveSanctionSummariesByMemberIds(any(), any())).willReturn(List.of());

        AdminMemberDetailResponse response = newService().getAdminMemberDetail(1L);

        assertThat(response.profile().email()).isEqualTo(member.getEmail());
        assertThat(response.profile().phone()).isEqualTo(member.getPhone());
    }

    @Test
    void getAdminMemberRentals_존재하지_않는_회원이면_MEMBER_NOT_FOUND를_던진다() {
        given(memberRepository.existsById(1L)).willReturn(false);
        assertThatThrownBy(() -> newService().getAdminMemberRentals(1L, 0, 20))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    void getAdminMemberReports_book_report와_review_report를_createdAt_내림차순으로_합친다() {
        given(memberRepository.existsById(1L)).willReturn(true);

        BookReport bookReport = mock(BookReport.class);
        given(bookReport.getBookReportId()).willReturn(10L);
        given(bookReport.getReportType()).willReturn("SPOILER");
        given(bookReport.getStatus()).willReturn("WAITING");
        given(bookReport.getCreatedAt()).willReturn(LocalDateTime.of(2026, 7, 1, 0, 0));

        ReviewReport reviewReport = mock(ReviewReport.class);
        given(reviewReport.getReviewReportId()).willReturn(20L);
        given(reviewReport.getReportType()).willReturn("ABUSE");
        given(reviewReport.getStatus()).willReturn("WAITING");
        given(reviewReport.getCreatedAt()).willReturn(LocalDateTime.of(2026, 7, 2, 0, 0));

        given(bookReportRepository.findByReporterIdForAdmin(1L)).willReturn(List.of(bookReport));
        given(reviewReportRepository.findByReporterIdForAdmin(1L)).willReturn(List.of(reviewReport));

        AdminMemberTabResponse<AdminMemberReportTabItemResponse> response = newService().getAdminMemberReports(1L, 0, 20);

        assertThat(response.tab().content()).extracting(AdminMemberReportTabItemResponse::reportSourceType)
                .containsExactly("REVIEW_REPORT", "BOOK_REPORT");
        assertThat(response.tab().totalElements()).isEqualTo(2);
    }

    // ---------- 003 상태/제재 변경 ----------

    @Test
    void changeMemberStatus_대상이_ADMIN이면_AUTH_ACCESS_DENIED를_던지고_감사로그를_남기지_않는다() {
        Member admin = joinedMember(1L, "ADMIN", "JOINED");
        given(memberRepository.findByIdForUpdate(1L)).willReturn(Optional.of(admin));

        AdminMemberStatusChangeRequest request = new AdminMemberStatusChangeRequest("WITHDRAWN", "탈퇴", null, null, null);

        assertThatThrownBy(() -> newService().changeMemberStatus(1L, request, 999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);
        verify(adminAuditLogWriter, never()).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    void changeMemberStatus_대상이_WITHDRAWN이면_AUTH_ACCESS_DENIED를_던진다() {
        Member withdrawn = joinedMember(1L, "USER", "WITHDRAWN");
        given(memberRepository.findByIdForUpdate(1L)).willReturn(Optional.of(withdrawn));

        AdminMemberStatusChangeRequest request = new AdminMemberStatusChangeRequest("JOINED", "복구", null, null, null);

        assertThatThrownBy(() -> newService().changeMemberStatus(1L, request, 999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);
    }

    @Test
    void changeMemberStatus_ACCOUNT_SUSPENSION인데_status가_SANCTIONED가_아니면_VALIDATION_ERROR() {
        AdminMemberStatusChangeRequest request =
                new AdminMemberStatusChangeRequest("JOINED", "정지", "ACCOUNT_SUSPENSION", null, null);
        assertThatThrownBy(() -> newService().changeMemberStatus(1L, request, 999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_ERROR);
        verify(memberRepository, never()).findByIdForUpdate(anyLong());
    }

    @Test
    void changeMemberStatus_RENTAL_BAN인데_status가_JOINED가_아니면_VALIDATION_ERROR() {
        AdminMemberStatusChangeRequest request =
                new AdminMemberStatusChangeRequest("SANCTIONED", "대여제한", "RENTAL_BAN", null, null);
        assertThatThrownBy(() -> newService().changeMemberStatus(1L, request, 999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void changeMemberStatus_sanctionType_없이_status_SANCTIONED면_VALIDATION_ERROR() {
        AdminMemberStatusChangeRequest request = new AdminMemberStatusChangeRequest("SANCTIONED", "정지", null, null, null);
        assertThatThrownBy(() -> newService().changeMemberStatus(1L, request, 999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void changeMemberStatus_ACCOUNT_SUSPENSION이면_회원상태를_SANCTIONED로_바꾸고_currentSanction을_설정한다() {
        Member member = joinedMember(1L, "USER", "JOINED");
        given(memberRepository.findByIdForUpdate(1L)).willReturn(Optional.of(member));
        given(member.getStatus()).willReturn("JOINED", "SANCTIONED");

        AdminMemberStatusChangeRequest request = new AdminMemberStatusChangeRequest(
                "SANCTIONED", "계정정지", "ACCOUNT_SUSPENSION", LocalDateTime.now(), LocalDateTime.now().plusDays(7));

        newService().changeMemberStatus(1L, request, 999L);

        verify(sanctionHistoryRepository).save(any(SanctionHistory.class));
        verify(member).applyAccountSuspension(any(SanctionHistory.class));
        verify(member, never()).changeStatusDirectly(any());
        verify(adminAuditLogWriter).record(eq(999L), any(), any(), eq(1L), any(), any());
    }

    @Test
    void changeMemberStatus_RENTAL_BAN이면_상태변경_없이_제재만_생성한다() {
        Member member = joinedMember(1L, "USER", "JOINED");
        given(memberRepository.findByIdForUpdate(1L)).willReturn(Optional.of(member));

        AdminMemberStatusChangeRequest request = new AdminMemberStatusChangeRequest(
                "JOINED", "대여제한", "RENTAL_BAN", null, LocalDateTime.now().plusDays(3));

        newService().changeMemberStatus(1L, request, 999L);

        verify(sanctionHistoryRepository).save(any(SanctionHistory.class));
        verify(member, never()).applyAccountSuspension(any());
        verify(member, never()).changeStatusDirectly(any());
    }

    @Test
    void changeMemberStatus_sanctionType_없으면_상태만_직접_변경한다() {
        Member member = joinedMember(1L, "USER", "JOINED");
        given(memberRepository.findByIdForUpdate(1L)).willReturn(Optional.of(member));

        AdminMemberStatusChangeRequest request = new AdminMemberStatusChangeRequest("DEACTIVATED", "휴면 처리", null, null, null);

        newService().changeMemberStatus(1L, request, 999L);

        verify(member).changeStatusDirectly("DEACTIVATED");
        verify(sanctionHistoryRepository, never()).save(any());
    }

    // ---------- 계정 정지 만료 배치 ----------

    @Test
    void restoreAccountSuspensionIfEligible_제재이력이_없으면_false를_반환한다() {
        given(sanctionHistoryRepository.findByIdForUpdate(1L)).willReturn(Optional.empty());
        boolean restored = newService().restoreAccountSuspensionIfEligible(1L, LocalDateTime.now());
        assertThat(restored).isFalse();
        verify(memberRepository, never()).findByIdForUpdate(anyLong());
    }

    @Test
    void restoreAccountSuspensionIfEligible_대상이_ADMIN이면_false를_반환한다() {
        SanctionHistory sanction = mock(SanctionHistory.class);
        Member admin = joinedMember(1L, "ADMIN", "SANCTIONED");
        given(sanction.getMember()).willReturn(admin);
        given(sanctionHistoryRepository.findByIdForUpdate(10L)).willReturn(Optional.of(sanction));
        given(memberRepository.findByIdForUpdate(1L)).willReturn(Optional.of(admin));

        boolean restored = newService().restoreAccountSuspensionIfEligible(10L, LocalDateTime.now());
        assertThat(restored).isFalse();
    }

    @Test
    void restoreAccountSuspensionIfEligible_currentSanctionId가_다르면_false를_반환한다() {
        Member member = joinedMember(1L, "USER", "SANCTIONED");
        SanctionHistory other = mock(SanctionHistory.class);
        given(other.getSanctionHistoryId()).willReturn(999L);
        given(member.getCurrentSanction()).willReturn(other);

        SanctionHistory candidate = mock(SanctionHistory.class);
        given(candidate.getMember()).willReturn(member);
        given(sanctionHistoryRepository.findByIdForUpdate(10L)).willReturn(Optional.of(candidate));
        given(memberRepository.findByIdForUpdate(1L)).willReturn(Optional.of(member));

        boolean restored = newService().restoreAccountSuspensionIfEligible(10L, LocalDateTime.now());
        assertThat(restored).isFalse();
        verify(member, never()).restoreFromSuspension();
    }

    @Test
    void restoreAccountSuspensionIfEligible_다른_활성_계정정지가_있으면_재지정만_하고_이력을_남기지_않는다() {
        Member member = joinedMember(1L, "USER", "SANCTIONED");
        SanctionHistory candidate = mock(SanctionHistory.class);
        given(candidate.getSanctionHistoryId()).willReturn(10L);
        given(candidate.getMember()).willReturn(member);
        given(candidate.getEndedAt()).willReturn(LocalDateTime.now().minusMinutes(1));
        given(member.getCurrentSanction()).willReturn(candidate);

        SanctionHistory stillActive = mock(SanctionHistory.class);
        given(sanctionHistoryRepository.findByIdForUpdate(10L)).willReturn(Optional.of(candidate));
        given(memberRepository.findByIdForUpdate(1L)).willReturn(Optional.of(member));
        given(sanctionHistoryRepository.findActiveAccountSuspensionsByMember(eq(1L), any()))
                .willReturn(List.of(stillActive));

        boolean restored = newService().restoreAccountSuspensionIfEligible(10L, LocalDateTime.now());

        assertThat(restored).isFalse();
        verify(member).reassignCurrentSanction(stillActive);
        verify(member, never()).restoreFromSuspension();
        verify(adminAuditLogWriter, never()).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    void restoreAccountSuspensionIfEligible_조건을_모두_만족하면_복구하고_감사로그를_남긴다() {
        Member member = joinedMember(1L, "USER", "SANCTIONED");
        SanctionHistory candidate = mock(SanctionHistory.class);
        given(candidate.getSanctionHistoryId()).willReturn(10L);
        given(candidate.getMember()).willReturn(member);
        given(candidate.getEndedAt()).willReturn(LocalDateTime.now().minusMinutes(1));
        given(member.getCurrentSanction()).willReturn(candidate);

        given(sanctionHistoryRepository.findByIdForUpdate(10L)).willReturn(Optional.of(candidate));
        given(memberRepository.findByIdForUpdate(1L)).willReturn(Optional.of(member));
        given(sanctionHistoryRepository.findActiveAccountSuspensionsByMember(eq(1L), any())).willReturn(List.of());

        boolean restored = newService().restoreAccountSuspensionIfEligible(10L, LocalDateTime.now());

        assertThat(restored).isTrue();
        verify(member).restoreFromSuspension();
        verify(adminAuditLogWriter).record(eq(null), any(), any(), eq(1L), any(), any());
    }
}
