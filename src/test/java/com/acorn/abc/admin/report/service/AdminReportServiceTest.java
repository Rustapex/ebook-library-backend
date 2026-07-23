package com.acorn.abc.admin.report.service;

import com.acorn.abc.admin.report.dto.AdminReportProcessRequest;
import com.acorn.abc.admin.report.dto.AdminReportProcessResponse;
import com.acorn.abc.admin.report.dto.AdminReportSanctionRequest;
import com.acorn.abc.admin.report.repository.AdminReportRepository;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.member.domain.Member;
import com.acorn.abc.member.repository.MemberRepository;
import com.acorn.abc.notification.repository.NotificationRepository;
import com.acorn.abc.review.domain.BookReport;
import com.acorn.abc.review.domain.Review;
import com.acorn.abc.review.domain.ReviewReport;
import com.acorn.abc.review.domain.SanctionHistory;
import com.acorn.abc.review.repository.BookReportRepository;
import com.acorn.abc.review.repository.ReviewReportRepository;
import com.acorn.abc.review.repository.SanctionHistoryRepository;
import com.acorn.abc.statistic.service.AdminAuditLogWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminReportServiceTest {

    private static final Long ADMIN_ID = 99L;
    private static final Long REVIEW_AUTHOR_ID = 500L;

    @Mock private AdminReportRepository adminReportRepository;
    @Mock private BookReportRepository bookReportRepository;
    @Mock private ReviewReportRepository reviewReportRepository;
    @Mock private SanctionHistoryRepository sanctionHistoryRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private AdminAuditLogWriter adminAuditLogWriter;

    @InjectMocks
    private AdminReportService adminReportService;

    // ---------- 목록 조회 ----------

    @Test
    void getAdminReports_잘못된_targetType이면_VALIDATION_ERROR를_던진다() {
        assertThatThrownBy(() -> adminReportService.getAdminReports("REVIEWER", null, 0, 10))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void getAdminReports_잘못된_status면_VALIDATION_ERROR를_던진다() {
        assertThatThrownBy(() -> adminReportService.getAdminReports(null, "UNKNOWN", 0, 10))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    // ---------- 처리 공통 검증 ----------

    @Test
    void processReport_targetType이_BOOK_REVIEW가_아니면_VALIDATION_ERROR를_던진다() {
        AdminReportProcessRequest request = new AdminReportProcessRequest("DONE", "처리완료", null, null);
        assertThatThrownBy(() -> adminReportService.processReport("MEMBER", 1L, request, ADMIN_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_ERROR);
        verify(bookReportRepository, never()).findByIdForUpdate(any());
        verify(reviewReportRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void processReport_상태값이_허용값이_아니면_VALIDATION_ERROR를_던진다() {
        AdminReportProcessRequest request = new AdminReportProcessRequest("CANCELED", null, null, null);
        assertThatThrownBy(() -> adminReportService.processReport("BOOK", 1L, request, ADMIN_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void processReport_최종처리인데_processResult가_없으면_VALIDATION_ERROR를_던진다() {
        AdminReportProcessRequest request = new AdminReportProcessRequest("DONE", "  ", null, null);
        assertThatThrownBy(() -> adminReportService.processReport("BOOK", 1L, request, ADMIN_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void processReport_BOOK_신고에_hideReviewYn이면_VALIDATION_ERROR를_던진다() {
        AdminReportProcessRequest request = new AdminReportProcessRequest("DONE", "처리완료", true, null);
        assertThatThrownBy(() -> adminReportService.processReport("BOOK", 1L, request, ADMIN_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void processReport_BOOK_신고에_sanction이_있으면_VALIDATION_ERROR를_던진다() {
        AdminReportSanctionRequest sanction = new AdminReportSanctionRequest("사유", LocalDateTime.now(), null);
        AdminReportProcessRequest request = new AdminReportProcessRequest("DONE", "처리완료", null, sanction);
        assertThatThrownBy(() -> adminReportService.processReport("BOOK", 1L, request, ADMIN_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void processReport_REVIEW_PROCESSING_처리인데_hideReviewYn이면_VALIDATION_ERROR를_던진다() {
        AdminReportProcessRequest request = new AdminReportProcessRequest("PROCESSING", null, true, null);
        assertThatThrownBy(() -> adminReportService.processReport("REVIEW", 1L, request, ADMIN_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void processReport_sanction_endedAt이_startedAt보다_이르면_VALIDATION_ERROR를_던진다() {
        LocalDateTime start = LocalDateTime.of(2026, 7, 22, 10, 0);
        AdminReportSanctionRequest sanction = new AdminReportSanctionRequest("사유", start, start.minusDays(1));
        AdminReportProcessRequest request = new AdminReportProcessRequest("DONE", "처리완료", null, sanction);
        assertThatThrownBy(() -> adminReportService.processReport("REVIEW", 1L, request, ADMIN_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    // ---------- BOOK 신고 처리 ----------

    @Test
    void processReport_BOOK_신고가_없으면_BOOK_REPORT_NOT_FOUND를_던진다() {
        given(bookReportRepository.findByIdForUpdate(1L)).willReturn(Optional.empty());
        AdminReportProcessRequest request = new AdminReportProcessRequest("DONE", "처리완료", null, null);

        assertThatThrownBy(() -> adminReportService.processReport("BOOK", 1L, request, ADMIN_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BOOK_REPORT_NOT_FOUND);
    }

    @Test
    void processReport_BOOK_신고의_허용되지_않은_상태전이는_REPORT_INVALID_STATUS_TRANSITION을_던진다() {
        BookReport report = mock(BookReport.class);
        given(report.getStatus()).willReturn("DONE");
        given(bookReportRepository.findByIdForUpdate(1L)).willReturn(Optional.of(report));
        AdminReportProcessRequest request = new AdminReportProcessRequest("PROCESSING", null, null, null);

        assertThatThrownBy(() -> adminReportService.processReport("BOOK", 1L, request, ADMIN_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.REPORT_INVALID_STATUS_TRANSITION);
        verify(report, never()).process(any(), any(), any());
    }

    @Test
    void processReport_BOOK_신고를_잠금조회로_가져와_WAITING에서_PROCESSING으로_정상_전이한다() {
        BookReport report = mock(BookReport.class);
        Member reporter = mock(Member.class);
        given(report.getStatus()).willReturn("WAITING");
        given(report.getReporter()).willReturn(reporter);
        given(bookReportRepository.findByIdForUpdate(1L)).willReturn(Optional.of(report));
        AdminReportProcessRequest request = new AdminReportProcessRequest("PROCESSING", null, null, null);

        AdminReportProcessResponse response = adminReportService.processReport("BOOK", 1L, request, ADMIN_ID);

        verify(bookReportRepository).findByIdForUpdate(1L);
        verify(report).process(eq("PROCESSING"), eq((String) null), any(LocalDateTime.class));
        verify(notificationRepository).save(any());
        verify(adminAuditLogWriter).record(any(), any(), any(), any(), any(), any());
        assertThat(response.targetType()).isEqualTo("BOOK");
        assertThat(response.reviewStatus()).isNull();
        assertThat(response.sanctionHistoryId()).isNull();
    }

    // ---------- REVIEW 신고 처리 ----------

    @Test
    void processReport_REVIEW_신고가_없으면_REVIEW_REPORT_NOT_FOUND를_던진다() {
        given(reviewReportRepository.findByIdForUpdate(1L)).willReturn(Optional.empty());
        AdminReportProcessRequest request = new AdminReportProcessRequest("DONE", "처리완료", null, null);

        assertThatThrownBy(() -> adminReportService.processReport("REVIEW", 1L, request, ADMIN_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.REVIEW_REPORT_NOT_FOUND);
    }

    @Test
    void processReport_REVIEW_신고를_잠금조회로_가져온다() {
        Review review = mock(Review.class);
        ReviewReport report = reviewReport("WAITING", review);
        given(reviewReportRepository.findByIdForUpdate(1L)).willReturn(Optional.of(report));
        AdminReportProcessRequest request = new AdminReportProcessRequest("PROCESSING", null, null, null);

        adminReportService.processReport("REVIEW", 1L, request, ADMIN_ID);

        verify(reviewReportRepository).findByIdForUpdate(1L);
        verify(memberRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void processReport_REVIEW_신고_DONE_hideReviewYn이면_리뷰를_숨김_처리한다() {
        Review review = mock(Review.class);
        given(review.getStatus()).willReturn("HIDDEN");
        ReviewReport report = reviewReport("WAITING", review);
        given(reviewReportRepository.findByIdForUpdate(1L)).willReturn(Optional.of(report));
        AdminReportProcessRequest request = new AdminReportProcessRequest("DONE", "숨김 처리", true, null);

        AdminReportProcessResponse response = adminReportService.processReport("REVIEW", 1L, request, ADMIN_ID);

        verify(review).hide();
        assertThat(response.reviewStatus()).isEqualTo("HIDDEN");
        assertThat(response.sanctionHistoryId()).isNull();
    }

    @Test
    void processReport_REVIEW_신고_DONE_sanction이_있으면_리뷰작성자를_잠근뒤_계정정지한다() {
        Member reviewAuthor = mock(Member.class);
        given(reviewAuthor.getMemberId()).willReturn(REVIEW_AUTHOR_ID);
        given(reviewAuthor.getStatus()).willReturn("SANCTIONED");
        given(reviewAuthor.isProtectedFromAdminChanges()).willReturn(false);
        given(memberRepository.findByIdForUpdate(REVIEW_AUTHOR_ID)).willReturn(Optional.of(reviewAuthor));

        Review review = mock(Review.class);
        given(review.getMember()).willReturn(reviewAuthor);
        ReviewReport report = reviewReport("PROCESSING", review);
        given(reviewReportRepository.findByIdForUpdate(1L)).willReturn(Optional.of(report));

        // save()는 IDENTITY 채번을 흉내내지 않고 전달받은 엔티티를 그대로 반환한다 — 실제 JPA save()가
        // 같은 인스턴스에 ID를 채워 돌려주는 동작을 단위 테스트 수준에서 재현한 것이다.
        given(sanctionHistoryRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        AdminReportSanctionRequest sanctionRequest =
                new AdminReportSanctionRequest("악성 리뷰", LocalDateTime.of(2026, 7, 22, 0, 0), null);
        AdminReportProcessRequest request = new AdminReportProcessRequest("DONE", "제재 완료", null, sanctionRequest);

        AdminReportProcessResponse response = adminReportService.processReport("REVIEW", 1L, request, ADMIN_ID);

        verify(memberRepository).findByIdForUpdate(REVIEW_AUTHOR_ID);
        ArgumentCaptor<SanctionHistory> sanctionCaptor = ArgumentCaptor.forClass(SanctionHistory.class);
        verify(sanctionHistoryRepository).save(sanctionCaptor.capture());
        SanctionHistory savedSanction = sanctionCaptor.getValue();
        assertThat(savedSanction.getMember()).isEqualTo(reviewAuthor);
        assertThat(savedSanction.getSanctionType()).isEqualTo("ACCOUNT_SUSPENSION");
        assertThat(savedSanction.getReason()).isEqualTo("악성 리뷰");

        verify(reviewAuthor).applyAccountSuspension(savedSanction);
        assertThat(response.memberStatus()).isEqualTo("SANCTIONED");
    }

    // PR64 코드리뷰 대응 §2.2 — 제재 대상이 ADMIN/WITHDRAWN/DEACTIVATED면 상태·리뷰·이력·알림·감사로그를
    // 만들기 전에 요청 전체를 403으로 거부해야 한다.
    @Test
    void processReport_제재대상이_보호회원이면_어떤_변경도_없이_AUTH_ACCESS_DENIED를_던진다() {
        Member reviewAuthor = mock(Member.class);
        given(reviewAuthor.getMemberId()).willReturn(REVIEW_AUTHOR_ID);
        given(reviewAuthor.isProtectedFromAdminChanges()).willReturn(true);
        given(memberRepository.findByIdForUpdate(REVIEW_AUTHOR_ID)).willReturn(Optional.of(reviewAuthor));

        Review review = mock(Review.class);
        given(review.getMember()).willReturn(reviewAuthor);
        ReviewReport report = reviewReport("WAITING", review);
        given(reviewReportRepository.findByIdForUpdate(1L)).willReturn(Optional.of(report));

        AdminReportSanctionRequest sanctionRequest =
                new AdminReportSanctionRequest("악성 리뷰", LocalDateTime.of(2026, 7, 22, 0, 0), null);
        AdminReportProcessRequest request = new AdminReportProcessRequest("DONE", "제재 완료", true, sanctionRequest);

        assertThatThrownBy(() -> adminReportService.processReport("REVIEW", 1L, request, ADMIN_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);

        verify(report, never()).process(any(), any(), any());
        verify(review, never()).hide();
        verify(sanctionHistoryRepository, never()).save(any());
        verify(reviewAuthor, never()).applyAccountSuspension(any());
        verify(notificationRepository, never()).save(any());
        verify(adminAuditLogWriter, never()).record(any(), any(), any(), any(), any(), any());
    }

    // 제재 요청이 없는 hideReviewYn 단독 처리는 리뷰 작성자의 보호 상태와 무관하게 허용된다(§2.2 "제재 요청이 있는 경우"만 해당).
    @Test
    void processReport_제재요청이_없으면_리뷰작성자가_보호회원이어도_숨김만_정상처리된다() {
        Member reviewAuthor = mock(Member.class);
        Review review = mock(Review.class);
        given(review.getMember()).willReturn(reviewAuthor);
        given(review.getStatus()).willReturn("HIDDEN");
        ReviewReport report = reviewReport("WAITING", review);
        given(reviewReportRepository.findByIdForUpdate(1L)).willReturn(Optional.of(report));

        AdminReportProcessRequest request = new AdminReportProcessRequest("DONE", "숨김 처리", true, null);

        AdminReportProcessResponse response = adminReportService.processReport("REVIEW", 1L, request, ADMIN_ID);

        verify(memberRepository, never()).findByIdForUpdate(any());
        verify(review).hide();
        assertThat(response.reviewStatus()).isEqualTo("HIDDEN");
    }

    @Test
    void processReport_REVIEW_신고를_REJECTED로_처리하면_리뷰숨김_제재_없이_상태만_바뀐다() {
        Review review = mock(Review.class);
        ReviewReport report = reviewReport("WAITING", review);
        given(reviewReportRepository.findByIdForUpdate(1L)).willReturn(Optional.of(report));
        AdminReportProcessRequest request = new AdminReportProcessRequest("REJECTED", "신고 사유 불충분", null, null);

        AdminReportProcessResponse response = adminReportService.processReport("REVIEW", 1L, request, ADMIN_ID);

        verify(review, never()).hide();
        verify(sanctionHistoryRepository, never()).save(any());
        assertThat(response.reviewStatus()).isNull();
        assertThat(response.sanctionHistoryId()).isNull();
    }

    private ReviewReport reviewReport(String status, Review review) {
        ReviewReport report = mock(ReviewReport.class);
        Member reporter = mock(Member.class);
        given(report.getStatus()).willReturn(status);
        given(report.getReporter()).willReturn(reporter);
        given(report.getReview()).willReturn(review);
        return report;
    }
}
