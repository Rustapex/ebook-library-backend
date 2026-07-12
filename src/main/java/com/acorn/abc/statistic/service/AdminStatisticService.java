package com.acorn.abc.statistic.service;

import com.acorn.abc.book.repository.BookRepository;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.member.repository.MemberRepository;
import com.acorn.abc.payment.repository.PaymentRepository;
import com.acorn.abc.rental.repository.BookRentalRepository;
import com.acorn.abc.rental.repository.ReadingProgressRepository;
import com.acorn.abc.review.domain.Review;
import com.acorn.abc.review.repository.BookReportRepository;
import com.acorn.abc.review.repository.ReviewReportRepository;
import com.acorn.abc.review.repository.ReviewRepository;
import com.acorn.abc.statistic.dto.AdminStatisticResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 관리자 통계를 실시간으로 집계한다(PR #37 리뷰 §1-3 반영 — 배치 사전집계 방식에서 전환).
 * 현재는 periodType=TOTAL, ageBand=ALL 조합만 지원한다. 그 외 조합은 COMMON_NOT_FOUND로 응답한다.
 * reviewCount는 활성(ACTIVE) 리뷰 수, reportCount는 도서/리뷰 신고 합계다(PR #41 리뷰 반영 — review/report Repository 도입 이후 실제 집계로 전환).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStatisticService {

    private static final String PERIOD_TYPE_TOTAL = "TOTAL";
    private static final String AGE_BAND_ALL = "ALL";
    // TOTAL 통계의 시작일 표기용 고정값. 실제 서비스 시작일이 아니라 "전체 기간"을 의미하는 상수다.
    private static final LocalDate TOTAL_PERIOD_START_DATE = LocalDate.of(2000, 1, 1);

    private final MemberRepository memberRepository;
    private final BookRepository bookRepository;
    private final BookRentalRepository bookRentalRepository;
    private final ReadingProgressRepository readingProgressRepository;
    private final PaymentRepository paymentRepository;
    private final ReviewRepository reviewRepository;
    private final BookReportRepository bookReportRepository;
    private final ReviewReportRepository reviewReportRepository;

    public AdminStatisticResponse getStatistics(String periodType, LocalDate baseDate, String ageBand) {
        String resolvedAgeBand = ageBand == null ? AGE_BAND_ALL : ageBand;
        return computeTotalStatistic(periodType, resolvedAgeBand);
    }

    public AdminStatisticResponse getTotalStatisticForDashboard() {
        return computeTotalStatistic(PERIOD_TYPE_TOTAL, AGE_BAND_ALL);
    }

    private AdminStatisticResponse computeTotalStatistic(String periodType, String ageBand) {
        if (!PERIOD_TYPE_TOTAL.equals(periodType) || !AGE_BAND_ALL.equals(ageBand)) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }

        long totalMemberCount = memberRepository.count();
        long sanctionedMemberCount = memberRepository.countByStatus("SANCTIONED");
        long activeBookCount = bookRepository.countByStatus("AVAILABLE");
        long totalRentalCount = bookRentalRepository.count();
        long freeRentalCount = bookRentalRepository.countByBook_RentalType("FREE");
        long paidRentalCount = bookRentalRepository.countByBook_RentalType("PAID");
        long totalReadBookCount = readingProgressRepository.countByCompletedYn(true);
        long totalPaymentAmount = paymentRepository.sumAmountByPaymentStatus("PAID");
        long reviewCount = reviewRepository.countByStatus(Review.STATUS_ACTIVE);
        long reportCount = bookReportRepository.count() + reviewReportRepository.count();

        return new AdminStatisticResponse(
                periodType,
                ageBand,
                TOTAL_PERIOD_START_DATE,
                LocalDate.now(),
                totalMemberCount,
                sanctionedMemberCount,
                activeBookCount,
                totalRentalCount,
                freeRentalCount,
                paidRentalCount,
                totalReadBookCount,
                totalPaymentAmount,
                reviewCount,
                reportCount,
                LocalDateTime.now());
    }
}
