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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 관리자 통계를 실시간으로 집계한다(PR #37 리뷰 §1-3 반영 — 배치 사전집계 방식에서 전환, 관리자 통계 확장 §3에서 기간/나이대/추이/환경지표까지 확장).
 * periodType=TOTAL/WEEKLY/MONTHLY/YEARLY, ageBand=ALL/10S/20S/30S/40S/50_PLUS 전 조합을 지원한다.
 * 회원 수/제재 회원 수/이용 가능 도서 수는 과거 시점 복원이 어려운 "현재 상태" 지표라 기간 필터를 적용하지 않는다(관리자 통계 확장 §3.3, 3.6 확정).
 * 추이(rentalTrend/paymentTrend/completedReadTrend)는 원본 이벤트 테이블 기준 실시간 bucket 집계이며, 스케줄러/스냅샷은 이번 범위에 포함하지 않는다(관리자 통계 확장 §3.6/§3.7 선택지 C 확정).
 * 기간/추이 계산은 UserStatisticService와 공통인 StatisticPeriodResolver/TrendBucketCalculator를 사용한다(관리자API+사용자통계 전체잔여작업 §3.1-2 확정).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStatisticService {

    private static final String AGE_BAND_ALL = "ALL";
    private static final long CARBON_KG_PER_COMPLETED_BOOK = 2L;
    private static final double TREE_PER_COMPLETED_BOOK = 0.04;

    private final MemberRepository memberRepository;
    private final BookRepository bookRepository;
    private final BookRentalRepository bookRentalRepository;
    private final ReadingProgressRepository readingProgressRepository;
    private final PaymentRepository paymentRepository;
    private final ReviewRepository reviewRepository;
    private final BookReportRepository bookReportRepository;
    private final ReviewReportRepository reviewReportRepository;
    private final StatisticPeriodResolver periodResolver;
    private final TrendBucketCalculator trendBucketCalculator;

    public AdminStatisticResponse getStatistics(String periodType, LocalDate baseDate, String ageBand) {
        String resolvedAgeBand = ageBand == null ? AGE_BAND_ALL : ageBand;
        return computeStatistic(periodType, baseDate, resolvedAgeBand);
    }

    public AdminStatisticResponse getTotalStatisticForDashboard() {
        return computeStatistic("TOTAL", null, AGE_BAND_ALL);
    }

    private AdminStatisticResponse computeStatistic(String periodType, LocalDate baseDate, String ageBand) {
        LocalDate resolvedBaseDate = baseDate == null ? LocalDate.now() : baseDate;
        StatisticPeriodResolver.PeriodRange periodRange = periodResolver.resolvePeriodRange(periodType, resolvedBaseDate);
        AgeBandRange ageBandRange = resolveAgeBandRange(ageBand, resolvedBaseDate);
        LocalDateTime from = periodRange.startDateTimeInclusive();
        LocalDateTime to = periodRange.endDateTimeExclusive();
        LocalDate birthDateFrom = ageBandRange.birthDateFromExclusive();
        LocalDate birthDateTo = ageBandRange.birthDateToInclusive();

        long totalMemberCount = memberRepository.countByAgeBand(birthDateFrom, birthDateTo);
        long sanctionedMemberCount = memberRepository.countByStatusAndAgeBand("SANCTIONED", birthDateFrom, birthDateTo);
        long activeBookCount = bookRepository.countByStatus("AVAILABLE");

        long totalRentalCount = bookRentalRepository.countByStatisticConditions(from, to, null, birthDateFrom, birthDateTo);
        long freeRentalCount = bookRentalRepository.countByStatisticConditions(from, to, "FREE", birthDateFrom, birthDateTo);
        long paidRentalCount = bookRentalRepository.countByStatisticConditions(from, to, "PAID", birthDateFrom, birthDateTo);

        long totalReadBookCount = readingProgressRepository.countCompletedByStatisticConditions(from, to, birthDateFrom, birthDateTo);
        long totalPaymentAmount = paymentRepository.sumAmountByStatisticConditions("PAID", from, to, birthDateFrom, birthDateTo);
        long reviewCount = reviewRepository.countByStatisticConditions(Review.STATUS_ACTIVE, from, to, birthDateFrom, birthDateTo);
        long reportCount = bookReportRepository.countByStatisticConditions(from, to, birthDateFrom, birthDateTo)
                + reviewReportRepository.countByStatisticConditions(from, to, birthDateFrom, birthDateTo);

        long carbonSavedKg = totalReadBookCount * CARBON_KG_PER_COMPLETED_BOOK;
        double treeSavedCount = totalReadBookCount * TREE_PER_COMPLETED_BOOK;

        List<TrendBucketCalculator.TrendBucket> buckets = trendBucketCalculator.resolveBuckets(periodType, resolvedBaseDate);
        List<AdminStatisticResponse.TrendCountPoint> rentalTrend = new ArrayList<>();
        List<AdminStatisticResponse.TrendAmountPoint> paymentTrend = new ArrayList<>();
        List<AdminStatisticResponse.TrendCountPoint> completedReadTrend = new ArrayList<>();

        // PR #42 리뷰 항목 2 반영: bucket마다 반복 조회하지 않고, 전체 추이 기간(trendFrom~trendTo)을
        // GROUP BY 쿼리 1회씩(대여/결제/완독 각 1회, 총 3회)으로 가져와 bucket label로 매칭한다.
        LocalDateTime trendFrom = buckets.get(0).startDateTimeInclusive();
        LocalDateTime trendTo = buckets.get(buckets.size() - 1).endDateTimeExclusive();
        boolean dailyGranularity = trendBucketCalculator.isDailyGranularity(periodType);

        Map<String, Long> rentalCountByLabel = dailyGranularity
                ? trendBucketCalculator.toBucketMap(bookRentalRepository.groupByDayStatisticConditions(trendFrom, trendTo, null, birthDateFrom, birthDateTo),
                        b -> b.getBucketDate().toString(), BookRentalRepository.DailyCountBucket::getBucketCount)
                : trendBucketCalculator.toBucketMap(bookRentalRepository.groupByMonthStatisticConditions(trendFrom, trendTo, null, birthDateFrom, birthDateTo),
                        BookRentalRepository.MonthlyCountBucket::getBucketMonth, BookRentalRepository.MonthlyCountBucket::getBucketCount);

        Map<String, Long> paymentAmountByLabel = dailyGranularity
                ? trendBucketCalculator.toBucketMap(paymentRepository.groupByDayStatisticConditions("PAID", trendFrom, trendTo, birthDateFrom, birthDateTo),
                        b -> b.getBucketDate().toString(), PaymentRepository.DailyAmountBucket::getBucketAmount)
                : trendBucketCalculator.toBucketMap(paymentRepository.groupByMonthStatisticConditions("PAID", trendFrom, trendTo, birthDateFrom, birthDateTo),
                        PaymentRepository.MonthlyAmountBucket::getBucketMonth, PaymentRepository.MonthlyAmountBucket::getBucketAmount);

        Map<String, Long> completedCountByLabel = dailyGranularity
                ? trendBucketCalculator.toBucketMap(readingProgressRepository.groupByDayCompletedStatisticConditions(trendFrom, trendTo, birthDateFrom, birthDateTo),
                        b -> b.getBucketDate().toString(), ReadingProgressRepository.DailyCountBucket::getBucketCount)
                : trendBucketCalculator.toBucketMap(readingProgressRepository.groupByMonthCompletedStatisticConditions(trendFrom, trendTo, birthDateFrom, birthDateTo),
                        ReadingProgressRepository.MonthlyCountBucket::getBucketMonth, ReadingProgressRepository.MonthlyCountBucket::getBucketCount);

        for (TrendBucketCalculator.TrendBucket bucket : buckets) {
            rentalTrend.add(new AdminStatisticResponse.TrendCountPoint(
                    bucket.label(), bucket.periodStartDate(), bucket.periodEndDate(),
                    rentalCountByLabel.getOrDefault(bucket.label(), 0L)));
            paymentTrend.add(new AdminStatisticResponse.TrendAmountPoint(
                    bucket.label(), bucket.periodStartDate(), bucket.periodEndDate(),
                    paymentAmountByLabel.getOrDefault(bucket.label(), 0L)));
            completedReadTrend.add(new AdminStatisticResponse.TrendCountPoint(
                    bucket.label(), bucket.periodStartDate(), bucket.periodEndDate(),
                    completedCountByLabel.getOrDefault(bucket.label(), 0L)));
        }

        return new AdminStatisticResponse(
                periodType,
                ageBand,
                periodRange.periodStartDate(),
                periodRange.periodEndDate(),
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
                carbonSavedKg,
                treeSavedCount,
                rentalTrend,
                paymentTrend,
                completedReadTrend,
                LocalDateTime.now());
    }

    // 만 나이 계산 기준(관리자 통계 확장 §3.2). birthDate <= 기준일-min, birthDate > 기준일-(max+1).
    private AgeBandRange resolveAgeBandRange(String ageBand, LocalDate baseDate) {
        if (AGE_BAND_ALL.equals(ageBand)) {
            return new AgeBandRange(null, null);
        }
        if ("10S".equals(ageBand)) {
            return ageBandRange(baseDate, 10, 19);
        }
        if ("20S".equals(ageBand)) {
            return ageBandRange(baseDate, 20, 29);
        }
        if ("30S".equals(ageBand)) {
            return ageBandRange(baseDate, 30, 39);
        }
        if ("40S".equals(ageBand)) {
            return ageBandRange(baseDate, 40, 49);
        }
        if ("50_PLUS".equals(ageBand)) {
            return ageBandRange(baseDate, 50, null);
        }
        // PR #42 리뷰 반영: 잘못된 ageBand이 조용히 ALL로 처리되지 않도록 fail-fast한다.
        throw new BusinessException(ErrorCode.VALIDATION_ERROR);
    }

    private AgeBandRange ageBandRange(LocalDate baseDate, int minAgeInclusive, Integer maxAgeInclusive) {
        LocalDate birthDateToInclusive = baseDate.minusYears(minAgeInclusive);
        LocalDate birthDateFromExclusive = maxAgeInclusive == null ? null : baseDate.minusYears(maxAgeInclusive + 1L);
        return new AgeBandRange(birthDateFromExclusive, birthDateToInclusive);
    }

    // birthDateFromExclusive/birthDateToInclusive 둘 다 null이면 나이대 필터 없음(ageBand=ALL)
    private record AgeBandRange(LocalDate birthDateFromExclusive, LocalDate birthDateToInclusive) {
    }
}
