package com.acorn.abc.admin.integration;

import com.acorn.abc.admin.report.dto.AdminReportProcessRequest;
import com.acorn.abc.admin.report.dto.AdminReportSanctionRequest;
import com.acorn.abc.admin.report.service.AdminReportService;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.member.dto.MemberWithdrawRequest;
import com.acorn.abc.member.service.MemberService;
import com.acorn.abc.payment.dto.AdminPaymentSummaryResponse;
import com.acorn.abc.payment.service.AdminPaymentService;
import com.acorn.abc.rental.dto.AdminRentalSummaryResponse;
import com.acorn.abc.rental.service.AdminRentalService;
import com.acorn.abc.admin.report.dto.AdminReportSummaryResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PR #64의 mock 테스트만으로 확인하기 어려운 저장·조회·감사 로그 경로를 실제 MySQL로 검증한다.
 * 각 실행은 UUID 접두어 데이터를 만들고 종료 시 해당 데이터만 삭제한다.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AdminOperationsRealDbIntegrationTest {

    private static final String PREFIX = "PR64_REAL_DB_";
    private static final String PASSWORD = "Passw0rd!";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AdminRentalService adminRentalService;

    @Autowired
    private AdminPaymentService adminPaymentService;

    @Autowired
    private AdminReportService adminReportService;

    @Autowired
    private MemberService memberService;

    private String suffix;
    private Long adminMemberId;
    private Long reporterMemberId;
    private Long reviewAuthorMemberId;
    private Long withdrawMemberId;
    private String withdrawLoginId;
    private Long bookId;
    private Long readyRentalId;
    private Long readingRentalId;
    private Long ownedRentalId;
    private Long paymentId;
    private Long reviewId;
    private Long bookReportId;
    private Long reviewReportId;
    private Long sharedReportId;
    private LocalDateTime sharedReportCreatedAt;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        adminMemberId = insertMember("admin_" + suffix, "ADMIN");
        reporterMemberId = insertMember("reporter_" + suffix, "USER");
        reviewAuthorMemberId = insertMember("author_" + suffix, "USER");
        withdrawLoginId = PREFIX + "withdraw_" + suffix;
        withdrawMemberId = insertMember(withdrawLoginId, "USER");
        bookId = insertBook();

        readyRentalId = insertRental(withdrawMemberId, "READY");
        readingRentalId = insertRental(withdrawMemberId, "READING");
        ownedRentalId = insertRental(withdrawMemberId, "OWNED");
        paymentId = insertPayment(ownedRentalId);

        reviewId = insertReview();
        sharedReportId = queryLong("SELECT GREATEST(COALESCE((SELECT MAX(book_report_id) FROM book_report), 0), " +
                "COALESCE((SELECT MAX(review_report_id) FROM review_report), 0)) + 1");
        sharedReportCreatedAt = LocalDateTime.now().plusHours(1);
        bookReportId = insertBookReport();
        reviewReportId = insertReviewReport();
    }

    @AfterEach
    void cleanUp() {
        if (reviewReportId != null) {
            jdbcTemplate.update("DELETE FROM admin_audit_log WHERE target_type = 'REVIEW_REPORT' AND target_id = ?", reviewReportId);
            jdbcTemplate.update("DELETE FROM notification WHERE target_type = 'REVIEW_REPORT' AND target_id = ?", reviewReportId);
        }
        if (bookReportId != null) {
            jdbcTemplate.update("DELETE FROM admin_audit_log WHERE target_type = 'BOOK_REPORT' AND target_id = ?", bookReportId);
            jdbcTemplate.update("DELETE FROM notification WHERE target_type = 'BOOK_REPORT' AND target_id = ?", bookReportId);
        }
        if (reviewAuthorMemberId != null) {
            jdbcTemplate.update("UPDATE member SET current_sanction_id = NULL WHERE member_id = ?", reviewAuthorMemberId);
        }
        if (reviewReportId != null) {
            jdbcTemplate.update("DELETE FROM sanction_history WHERE review_report_id = ?", reviewReportId);
            jdbcTemplate.update("DELETE FROM review_report WHERE review_report_id = ?", reviewReportId);
        }
        if (bookReportId != null) {
            jdbcTemplate.update("DELETE FROM book_report WHERE book_report_id = ?", bookReportId);
        }
        if (reviewId != null) {
            jdbcTemplate.update("DELETE FROM review WHERE review_id = ?", reviewId);
        }
        if (paymentId != null) {
            jdbcTemplate.update("DELETE FROM payment WHERE payment_id = ?", paymentId);
        }
        jdbcTemplate.update("DELETE FROM book_rental WHERE member_id = ?", withdrawMemberId);
        jdbcTemplate.update("DELETE FROM member WHERE member_id IN (?, ?, ?, ?)",
                adminMemberId, reporterMemberId, reviewAuthorMemberId, withdrawMemberId);
        if (bookId != null) {
            jdbcTemplate.update("DELETE FROM book WHERE book_id = ?", bookId);
        }
    }

    @Test
    void 관리자_대여와_결제_목록은_이번_실행의_저장_데이터를_조회한다() {
        PageResponse<AdminRentalSummaryResponse> rentals =
                adminRentalService.getAdminRentals(null, withdrawMemberId, null, 0, 20);
        PageResponse<AdminPaymentSummaryResponse> payments =
                adminPaymentService.getAdminPayments(PREFIX + "name", LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), 0, 20);
        PageResponse<AdminRentalSummaryResponse> readyRentals =
                adminRentalService.getAdminRentals(null, withdrawMemberId, "READY", 0, 20);
        PageResponse<AdminPaymentSummaryResponse> outOfRangePayments =
                adminPaymentService.getAdminPayments(PREFIX + "name", LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), 0, 20);

        assertThat(rentals.content()).extracting(AdminRentalSummaryResponse::rentalId)
                .contains(readyRentalId, readingRentalId, ownedRentalId);
        assertThat(rentals.content()).filteredOn(rental -> rental.rentalId().equals(ownedRentalId))
                .singleElement()
                .extracting(rental -> rental.payment().paymentId())
                .isEqualTo(paymentId);
        assertThat(payments.content()).extracting(AdminPaymentSummaryResponse::paymentId).contains(paymentId);
        assertThat(readyRentals.content()).extracting(AdminRentalSummaryResponse::rentalId).containsExactly(readyRentalId);
        assertThat(outOfRangePayments.content()).isEmpty();
    }

    @Test
    void 관리자_신고_처리는_리뷰숨김_계정정지_알림_감사로그를_하나의_DB_흐름으로_저장한다() {
        PageResponse<AdminReportSummaryResponse> waitingReports =
                adminReportService.getAdminReports(null, "WAITING", 0, 100);
        PageResponse<AdminReportSummaryResponse> firstPage =
                adminReportService.getAdminReports(null, "WAITING", 0, 1);
        PageResponse<AdminReportSummaryResponse> secondPage =
                adminReportService.getAdminReports(null, "WAITING", 1, 1);
        assertThat(waitingReports.content()).extracting(AdminReportSummaryResponse::reportId)
                .contains(bookReportId, reviewReportId);
        assertThat(firstPage.content()).singleElement().satisfies(report -> {
            assertThat(report.reportId()).isEqualTo(sharedReportId);
            assertThat(report.targetType()).isEqualTo("BOOK");
        });
        assertThat(secondPage.content()).singleElement().satisfies(report -> {
            assertThat(report.reportId()).isEqualTo(sharedReportId);
            assertThat(report.targetType()).isEqualTo("REVIEW");
        });

        assertThatThrownBy(() -> adminReportService.processReport("BOOK", bookReportId,
                new AdminReportProcessRequest("DONE", "잘못된 BOOK 처리", true, null), adminMemberId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThat(queryString("SELECT status FROM book_report WHERE book_report_id = ?", bookReportId)).isEqualTo("WAITING");

        adminReportService.processReport("BOOK", bookReportId,
                new AdminReportProcessRequest("DONE", "도서 신고 처리", null, null), adminMemberId);

        adminReportService.processReport("REVIEW", reviewReportId,
                new AdminReportProcessRequest("DONE", "리뷰 신고 처리", true,
                        new AdminReportSanctionRequest("테스트 계정 정지", LocalDateTime.now(), LocalDateTime.now().plusDays(1))),
                adminMemberId);

        assertThat(queryString("SELECT status FROM book_report WHERE book_report_id = ?", bookReportId)).isEqualTo("DONE");
        assertThat(queryString("SELECT status FROM review_report WHERE review_report_id = ?", reviewReportId)).isEqualTo("DONE");
        assertThat(queryString("SELECT status FROM review WHERE review_id = ?", reviewId)).isEqualTo("HIDDEN");
        assertThat(queryString("SELECT status FROM member WHERE member_id = ?", reviewAuthorMemberId)).isEqualTo("SANCTIONED");
        assertThat(queryLong("SELECT COUNT(*) FROM sanction_history WHERE review_report_id = ?", reviewReportId)).isEqualTo(1L);
        assertThat(queryLong("SELECT COUNT(*) FROM notification WHERE target_type = 'REVIEW_REPORT' AND target_id = ?", reviewReportId)).isEqualTo(1L);
        assertThat(queryLong("SELECT COUNT(*) FROM admin_audit_log WHERE target_type = 'REVIEW_REPORT' AND target_id = ?", reviewReportId)).isEqualTo(1L);

        assertThatThrownBy(() -> adminReportService.processReport("REVIEW", reviewReportId,
                new AdminReportProcessRequest("DONE", "중복 처리", true, null), adminMemberId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.REPORT_INVALID_STATUS_TRANSITION);
    }

    @Test
    void 회원탈퇴는_진행중대여를_거부하고_OWNED만_남으면_탈퇴하며_ADMIN은_항상_거부한다() {
        MemberWithdrawRequest request = new MemberWithdrawRequest(PASSWORD, "실제 DB 탈퇴 테스트");

        assertThatThrownBy(() -> memberService.withdraw(withdrawMemberId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.MEMBER_WITHDRAWAL_RENTAL_IN_PROGRESS);

        jdbcTemplate.update("DELETE FROM book_rental WHERE rental_id IN (?, ?)", readyRentalId, readingRentalId);

        memberService.withdraw(withdrawMemberId, request);

        assertThat(queryString("SELECT status FROM member WHERE member_id = ?", withdrawMemberId)).isEqualTo("WITHDRAWN");
        assertThat(queryLong("SELECT COUNT(*) FROM book_rental WHERE rental_id = ? AND rental_status = 'OWNED'", ownedRentalId)).isEqualTo(1L);
        assertThatThrownBy(() -> memberService.withdraw(adminMemberId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);
    }

    private Long insertMember(String loginId, String role) {
        jdbcTemplate.update("INSERT INTO member (login_id, password_hash, name, email, birth_date, role, grade_id, point_balance, status, created_at) " +
                        "VALUES (?, ?, ?, ?, '2000-01-01', ?, 1, 0, 'JOINED', NOW())",
                loginId, passwordEncoder.encode(PASSWORD), PREFIX + "name", loginId + "@example.test", role);
        return queryLong("SELECT member_id FROM member WHERE login_id = ?", loginId);
    }

    private Long insertBook() {
        String title = PREFIX + "book_" + suffix;
        jdbcTemplate.update("INSERT INTO book (title, rental_type, rental_price, default_rental_days, total_pages, status, created_at) " +
                "VALUES (?, 'PAID', 1000, 7, 10, 'AVAILABLE', NOW())", title);
        return queryLong("SELECT book_id FROM book WHERE title = ?", title);
    }

    private Long insertRental(Long memberId, String status) {
        jdbcTemplate.update("INSERT INTO book_rental (member_id, book_id, rental_start_at, rental_end_at, rental_status, created_at) " +
                        "VALUES (?, ?, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY), ?, NOW())",
                memberId, bookId, status);
        return queryLong("SELECT rental_id FROM book_rental WHERE member_id = ? AND book_id = ? AND rental_status = ? " +
                "ORDER BY rental_id DESC LIMIT 1", memberId, bookId, status);
    }

    private Long insertPayment(Long rentalId) {
        jdbcTemplate.update("INSERT INTO payment (member_id, rental_id, payment_type, original_amount, amount, payment_method, payment_status, paid_at, created_at) " +
                        "VALUES (?, ?, 'RENTAL', 1000, 1000, 'CARD', 'PAID', NOW(), NOW())",
                withdrawMemberId, rentalId);
        return queryLong("SELECT payment_id FROM payment WHERE rental_id = ?", rentalId);
    }

    private Long insertReview() {
        jdbcTemplate.update("INSERT INTO review (member_id, book_id, content, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, 'ACTIVE', NOW(), NOW())",
                reviewAuthorMemberId, bookId, PREFIX + "review_" + suffix);
        return queryLong("SELECT review_id FROM review WHERE member_id = ? AND book_id = ?", reviewAuthorMemberId, bookId);
    }

    private Long insertBookReport() {
        jdbcTemplate.update("INSERT INTO book_report (book_report_id, reporter_id, book_id, report_type, content, status, created_at) " +
                        "VALUES (?, ?, ?, 'OTHER', ?, 'WAITING', ?)",
                sharedReportId, reporterMemberId, bookId, PREFIX + "book_report_" + suffix, sharedReportCreatedAt);
        return sharedReportId;
    }

    private Long insertReviewReport() {
        jdbcTemplate.update("INSERT INTO review_report (review_report_id, reporter_id, review_id, report_type, content, status, created_at) " +
                        "VALUES (?, ?, ?, 'OTHER', ?, 'WAITING', ?)",
                sharedReportId, reporterMemberId, reviewId, PREFIX + "review_report_" + suffix, sharedReportCreatedAt);
        return sharedReportId;
    }

    private Long queryLong(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Long.class, args);
    }

    private String queryString(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, String.class, args);
    }
}
