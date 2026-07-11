package com.acorn.abc.statistic.service;

import com.acorn.abc.payment.repository.PaymentRepository;
import com.acorn.abc.request.repository.BookRequestCandidateRepository;
import com.acorn.abc.review.repository.BookReportRepository;
import com.acorn.abc.review.repository.ReviewReportRepository;
import com.acorn.abc.statistic.dto.AdminDashboardResponse;
import com.acorn.abc.statistic.dto.AdminRecentBookRequestResponse;
import com.acorn.abc.statistic.dto.AdminRecentPaymentResponse;
import com.acorn.abc.statistic.dto.AdminRecentReportResponse;
import com.acorn.abc.statistic.dto.AdminStatisticResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private static final int RECENT_PAYMENT_LIMIT = 5;
    private static final int RECENT_REPORT_LIMIT = 5;
    private static final int RECENT_BOOK_REQUEST_LIMIT = 5;

    private final AdminStatisticService adminStatisticService;
    private final PaymentRepository paymentRepository;
    private final BookReportRepository bookReportRepository;
    private final ReviewReportRepository reviewReportRepository;
    private final BookRequestCandidateRepository bookRequestCandidateRepository;

    public AdminDashboardResponse getDashboard() {
        AdminStatisticResponse statistics = adminStatisticService.getTotalStatisticForDashboard();

        Pageable paymentPageable = PageRequest.of(0, RECENT_PAYMENT_LIMIT);
        List<AdminRecentPaymentResponse> recentPayments = paymentRepository.findRecentPayments(paymentPageable).stream()
                .map(AdminRecentPaymentResponse::from)
                .toList();

        List<AdminRecentReportResponse> recentReports = getRecentReports();

        Pageable bookRequestPageable = PageRequest.of(0, RECENT_BOOK_REQUEST_LIMIT);
        List<AdminRecentBookRequestResponse> recentBookRequests = bookRequestCandidateRepository
                .findAllByOrderByFirstRequestedAtDesc(bookRequestPageable).stream()
                .map(AdminRecentBookRequestResponse::from)
                .toList();

        return new AdminDashboardResponse(statistics, recentPayments, recentReports, recentBookRequests);
    }

    // 책 신고/리뷰 신고를 각각 최근 N건 조회한 뒤 서비스에서 합쳐 최신순으로 다시 자른다(구현계획 §6.2).
    private List<AdminRecentReportResponse> getRecentReports() {
        Pageable fetchPageable = PageRequest.of(0, RECENT_REPORT_LIMIT);

        Stream<AdminRecentReportResponse> bookReports = bookReportRepository.findRecent(fetchPageable).stream()
                .map(AdminRecentReportResponse::fromBookReport);
        Stream<AdminRecentReportResponse> reviewReports = reviewReportRepository.findRecent(fetchPageable).stream()
                .map(AdminRecentReportResponse::fromReviewReport);

        return Stream.concat(bookReports, reviewReports)
                .sorted(Comparator.comparing(AdminRecentReportResponse::createdAt).reversed())
                .limit(RECENT_REPORT_LIMIT)
                .toList();
    }
}
