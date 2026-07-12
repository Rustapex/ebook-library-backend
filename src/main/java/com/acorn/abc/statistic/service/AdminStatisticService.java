package com.acorn.abc.statistic.service;

import com.acorn.abc.book.repository.BookRepository;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * 관리자 통계를 실시간으로 집계한다(PR #37 리뷰 §1-3 반영 — 배치 사전집계 방식에서 전환, 관리자 통계 확장 §3에서 기간/나이대/추이/환경지표까지 확장).
 * periodType=TOTAL/WEEKLY/MONTHLY/YEARLY, ageBand=ALL/10S/20S/30S/40S/50_PLUS 전 조합을 지원한다.
 * 회원 수/제재 회원 수/이용 가능 도서 수는 과거 시점 복원이 어려운 "현재 상태" 지표라 기간 필터를 적용하지 않는다(관리자 통계 확장 §3.3, 3.6 확정).
 * 추이(rentalTrend/paymentTrend/completedReadTrend)는 원본 이벤트 테이블 기준 실시간 bucket 집계이며, 스케줄러/스냅샷은 이번 범위에 포함하지 않는다(관리자 통계 확장 §3.6/§3.7 선택지 C 확정).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStatisticService {

    private static final String PERIOD_TYPE_TOTAL = "TOTAL";
    private static final String PERIOD_TYPE_WEEKLY = "WEEKLY";
    private static final String PERIOD_TYPE_MONTHLY = "MONTHLY";
    private static final String PERIOD_TYPE_YEARLY = "YEARLY";
    private static final String AGE_BAND_ALL = "ALL";
    // TOTAL 통계의 시작일 표기용 고정값. 실제 서비스 시작일이 아니라 "전체 기간"을 의미하는 상수다.
    private static final LocalDate TOTAL_PERIOD_START_DATE = LocalDate.of(2000, 1, 1);
    private static final long CARBON_KG_PER_COMPLETED_BOOK = 2L;
    private static final double TREE_PER_COMPLETED_BOOK = 0.04;
    private static final int TOTAL_TREND_MONTH_COUNT = 12;

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
        return computeStatistic(periodType, baseDate, resolvedAgeBand);
    }

    public AdminStatisticResponse getTotalStatisticForDashboard() {
        return computeStatistic(PERIOD_TYPE_TOTAL, null, AGE_BAND_ALL);
    }

    private AdminStatisticResponse computeStatistic(String periodType, LocalDate baseDate, String ageBand) {
        LocalDate resolvedBaseDate = baseDate == null ? LocalDate.now() : baseDate;
        PeriodRange periodRange = resolvePeriodRange(periodType, resolvedBaseDate);
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

        List<TrendBucket> buckets = resolveTrendBuckets(periodType, resolvedBaseDate);
        List<AdminStatisticResponse.TrendCountPoint> rentalTrend = new ArrayList<>();
        List<AdminStatisticResponse.TrendAmountPoint> paymentTrend = new ArrayList<>();
        List<AdminStatisticResponse.TrendCountPoint> completedReadTrend = new ArrayList<>();
        for (TrendBucket bucket : buckets) {
            long bucketRentalCount = bookRentalRepository.countByStatisticConditions(
                    bucket.startDateTimeInclusive(), bucket.endDateTimeExclusive(), null, birthDateFrom, birthDateTo);
            rentalTrend.add(new AdminStatisticResponse.TrendCountPoint(
                    bucket.label(), bucket.periodStartDate(), bucket.periodEndDate(), bucketRentalCount));

            long bucketPaymentAmount = paymentRepository.sumAmountByStatisticConditions("PAID",
                    bucket.startDateTimeInclusive(), bucket.endDateTimeExclusive(), birthDateFrom, birthDateTo);
            paymentTrend.add(new AdminStatisticResponse.TrendAmountPoint(
                    bucket.label(), bucket.periodStartDate(), bucket.periodEndDate(), bucketPaymentAmount));

            long bucketCompletedCount = readingProgressRepository.countCompletedByStatisticConditions(
                    bucket.startDateTimeInclusive(), bucket.endDateTimeExclusive(), birthDateFrom, birthDateTo);
            completedReadTrend.add(new AdminStatisticResponse.TrendCountPoint(
                    bucket.label(), bucket.periodStartDate(), bucket.periodEndDate(), bucketCompletedCount));
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

    // 기간 계산 기준(관리자 통계 확장 §3.1). start <= timestamp < endExclusive로 통일한다.
    private PeriodRange resolvePeriodRange(String periodType, LocalDate baseDate) {
        if (PERIOD_TYPE_WEEKLY.equals(periodType)) {
            LocalDate monday = baseDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate nextMonday = monday.plusWeeks(1);
            return new PeriodRange(monday, nextMonday.minusDays(1), monday.atStartOfDay(), nextMonday.atStartOfDay());
        }
        if (PERIOD_TYPE_MONTHLY.equals(periodType)) {
            LocalDate firstOfMonth = baseDate.withDayOfMonth(1);
            LocalDate firstOfNextMonth = firstOfMonth.plusMonths(1);
            return new PeriodRange(firstOfMonth, firstOfNextMonth.minusDays(1), firstOfMonth.atStartOfDay(), firstOfNextMonth.atStartOfDay());
        }
        if (PERIOD_TYPE_YEARLY.equals(periodType)) {
            LocalDate firstOfYear = LocalDate.of(baseDate.getYear(), 1, 1);
            LocalDate firstOfNextYear = firstOfYear.plusYears(1);
            return new PeriodRange(firstOfYear, firstOfNextYear.minusDays(1), firstOfYear.atStartOfDay(), firstOfNextYear.atStartOfDay());
        }
        // TOTAL: baseDate가 있어도 집계 기간에는 영향을 주지 않는다. periodEndDate는 요청 시점의 오늘 날짜.
        return new PeriodRange(TOTAL_PERIOD_START_DATE, LocalDate.now(), null, null);
    }

    // 만 나이 계산 기준(관리자 통계 확장 §3.2). birthDate <= 기준일-min, birthDate > 기준일-(max+1).
    private AgeBandRange resolveAgeBandRange(String ageBand, LocalDate baseDate) {
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
        return new AgeBandRange(null, null);
    }

    private AgeBandRange ageBandRange(LocalDate baseDate, int minAgeInclusive, Integer maxAgeInclusive) {
        LocalDate birthDateToInclusive = baseDate.minusYears(minAgeInclusive);
        LocalDate birthDateFromExclusive = maxAgeInclusive == null ? null : baseDate.minusYears(maxAgeInclusive + 1L);
        return new AgeBandRange(birthDateFromExclusive, birthDateToInclusive);
    }

    // 추이 bucket 기준(관리자 통계 확장 §3.4, §3.6 확정): WEEKLY=7일 일별, MONTHLY=해당 월 일별,
    // YEARLY=12개월 월별, TOTAL=baseDate가 속한 달까지의 최근 12개월 월별.
    private List<TrendBucket> resolveTrendBuckets(String periodType, LocalDate baseDate) {
        if (PERIOD_TYPE_WEEKLY.equals(periodType)) {
            LocalDate monday = baseDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            List<TrendBucket> buckets = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                buckets.add(dailyBucket(monday.plusDays(i)));
            }
            return buckets;
        }
        if (PERIOD_TYPE_MONTHLY.equals(periodType)) {
            LocalDate firstOfMonth = baseDate.withDayOfMonth(1);
            int lengthOfMonth = YearMonth.from(baseDate).lengthOfMonth();
            List<TrendBucket> buckets = new ArrayList<>();
            for (int i = 0; i < lengthOfMonth; i++) {
                buckets.add(dailyBucket(firstOfMonth.plusDays(i)));
            }
            return buckets;
        }
        if (PERIOD_TYPE_YEARLY.equals(periodType)) {
            List<TrendBucket> buckets = new ArrayList<>();
            for (int month = 1; month <= 12; month++) {
                buckets.add(monthlyBucket(YearMonth.of(baseDate.getYear(), month)));
            }
            return buckets;
        }
        // TOTAL: 최근 12개월 월별(baseDate가 속한 달 포함)
        YearMonth endMonth = YearMonth.from(baseDate);
        List<TrendBucket> buckets = new ArrayList<>();
        for (int i = TOTAL_TREND_MONTH_COUNT - 1; i >= 0; i--) {
            buckets.add(monthlyBucket(endMonth.minusMonths(i)));
        }
        return buckets;
    }

    private TrendBucket dailyBucket(LocalDate day) {
        return new TrendBucket(day.toString(), day, day, day.atStartOfDay(), day.plusDays(1).atStartOfDay());
    }

    private TrendBucket monthlyBucket(YearMonth yearMonth) {
        LocalDate first = yearMonth.atDay(1);
        LocalDate firstOfNext = yearMonth.plusMonths(1).atDay(1);
        String label = String.format("%04d-%02d", yearMonth.getYear(), yearMonth.getMonthValue());
        return new TrendBucket(label, first, firstOfNext.minusDays(1), first.atStartOfDay(), firstOfNext.atStartOfDay());
    }

    private record PeriodRange(LocalDate periodStartDate, LocalDate periodEndDate,
                                LocalDateTime startDateTimeInclusive, LocalDateTime endDateTimeExclusive) {
    }

    // birthDateFromExclusive/birthDateToInclusive 둘 다 null이면 나이대 필터 없음(ageBand=ALL)
    private record AgeBandRange(LocalDate birthDateFromExclusive, LocalDate birthDateToInclusive) {
    }

    private record TrendBucket(String label, LocalDate periodStartDate, LocalDate periodEndDate,
                                LocalDateTime startDateTimeInclusive, LocalDateTime endDateTimeExclusive) {
    }
}
