package com.acorn.abc.statistic.service;

import com.acorn.abc.book.domain.Book;
import com.acorn.abc.member.domain.Member;
import com.acorn.abc.payment.domain.Payment;
import com.acorn.abc.payment.repository.PaymentRepository;
import com.acorn.abc.request.domain.BookRequestCandidate;
import com.acorn.abc.request.repository.BookRequestCandidateRepository;
import com.acorn.abc.review.domain.BookReport;
import com.acorn.abc.review.domain.Review;
import com.acorn.abc.review.domain.ReviewReport;
import com.acorn.abc.review.repository.BookReportRepository;
import com.acorn.abc.review.repository.ReviewReportRepository;
import com.acorn.abc.statistic.dto.AdminDashboardResponse;
import com.acorn.abc.statistic.dto.AdminStatisticResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private AdminStatisticService adminStatisticService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookReportRepository bookReportRepository;

    @Mock
    private ReviewReportRepository reviewReportRepository;

    @Mock
    private BookRequestCandidateRepository bookRequestCandidateRepository;

    @InjectMocks
    private AdminDashboardService adminDashboardService;

    @Test
    void getDashboard_통계와_최근_결제_신고_희망도서를_함께_반환한다() {
        AdminStatisticResponse statistics = new AdminStatisticResponse(
                "TOTAL", "ALL", LocalDate.of(2000, 1, 1), LocalDate.now(),
                5, 0, 1, 10, 7, 3, 4, 19800L, 0, 0, LocalDateTime.now());
        given(adminStatisticService.getTotalStatisticForDashboard()).willReturn(statistics);

        Member member = mock(Member.class);
        lenient().when(member.getName()).thenReturn("홍길동");
        Payment payment = mock(Payment.class);
        given(payment.getPaymentId()).willReturn(1L);
        given(payment.getMember()).willReturn(member);
        given(payment.getAmount()).willReturn(9900);
        given(payment.getPaymentStatus()).willReturn("PAID");
        given(payment.getPaidAt()).willReturn(LocalDateTime.now());
        given(paymentRepository.findRecentPayments(any())).willReturn(List.of(payment));

        Book book = mock(Book.class);
        given(book.getBookId()).willReturn(10L);
        given(book.getTitle()).willReturn("테스트 도서");
        BookReport bookReport = mock(BookReport.class);
        given(bookReport.getBookReportId()).willReturn(100L);
        given(bookReport.getBook()).willReturn(book);
        given(bookReport.getReportType()).willReturn("SPAM");
        given(bookReport.getStatus()).willReturn("WAITING");
        given(bookReport.getReporter()).willReturn(member);
        given(bookReport.getCreatedAt()).willReturn(LocalDateTime.now().minusHours(1));
        given(bookReportRepository.findRecent(any())).willReturn(List.of(bookReport));

        Review review = mock(Review.class);
        given(review.getReviewId()).willReturn(20L);
        given(review.getBook()).willReturn(book);
        ReviewReport reviewReport = mock(ReviewReport.class);
        given(reviewReport.getReviewReportId()).willReturn(200L);
        given(reviewReport.getReview()).willReturn(review);
        given(reviewReport.getReportType()).willReturn("ABUSE");
        given(reviewReport.getStatus()).willReturn("WAITING");
        given(reviewReport.getReporter()).willReturn(member);
        given(reviewReport.getCreatedAt()).willReturn(LocalDateTime.now());
        given(reviewReportRepository.findRecent(any())).willReturn(List.of(reviewReport));

        BookRequestCandidate candidate = mock(BookRequestCandidate.class);
        given(candidate.getRequestCandidateId()).willReturn(300L);
        given(candidate.getNormalizedTitle()).willReturn("희망도서 제목");
        given(candidate.getNormalizedAuthor()).willReturn("저자");
        given(candidate.getNormalizedPublisher()).willReturn("출판사");
        given(candidate.getRequestCount()).willReturn(3);
        given(candidate.getCandidateStatus()).willReturn("REQUESTED");
        given(candidate.getFirstRequestedAt()).willReturn(LocalDateTime.now());
        given(bookRequestCandidateRepository.findAllByOrderByFirstRequestedAtDesc(any()))
                .willReturn(List.of(candidate));

        AdminDashboardResponse response = adminDashboardService.getDashboard();

        assertThat(response.statistics().periodType()).isEqualTo("TOTAL");
        assertThat(response.recentPayments()).hasSize(1);
        assertThat(response.recentPayments().get(0).memberName()).isEqualTo("홍길동");
        assertThat(response.recentPayments().get(0).amount()).isEqualTo(9900);

        // 리뷰 신고가 더 최근이므로 먼저 나와야 한다(서비스에서 최신순으로 합치는지 확인).
        assertThat(response.recentReports()).hasSize(2);
        assertThat(response.recentReports().get(0).targetType()).isEqualTo("REVIEW");
        assertThat(response.recentReports().get(0).targetTitle()).isEqualTo("테스트 도서");
        assertThat(response.recentReports().get(1).targetType()).isEqualTo("BOOK");

        assertThat(response.recentBookRequests()).hasSize(1);
        assertThat(response.recentBookRequests().get(0).title()).isEqualTo("희망도서 제목");
        assertThat(response.recentBookRequests().get(0).requestCount()).isEqualTo(3);
    }
}
