package com.acorn.abc.statistic.service;

import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.favorite.repository.FavoriteBookRepository;
import com.acorn.abc.rental.repository.BookRentalRepository;
import com.acorn.abc.rental.repository.ReadingProgressRepository;
import com.acorn.abc.review.domain.Review;
import com.acorn.abc.review.repository.ReviewRepository;
import com.acorn.abc.statistic.dto.UserReadingStatisticResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 회원 본인 독서 통계를 실시간으로 집계한다(회원용독서통계 구현계획 §4.1 — 관리자 통계와 동일한
 * 실시간 집계 정책 적용, user_reading_statistic 테이블/배치는 쓰지 않는다).
 * "읽은 페이지"는 last_read_at 기간 필터 기준 근사치다(§4.2, B1 확정).
 * 즐겨찾기 수는 기간과 무관한 현재 상태 지표다(§4.4 확정).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserStatisticService {

    private static final String PERIOD_TYPE_TOTAL = "TOTAL";
    private static final String PERIOD_TYPE_WEEKLY = "WEEKLY";
    private static final String PERIOD_TYPE_MONTHLY = "MONTHLY";
    private static final String PERIOD_TYPE_YEARLY = "YEARLY";
    // TOTAL 통계의 시작일 표기용 고정값. 실제 서비스 시작일이 아니라 "전체 기간"을 의미하는 상수다.
    private static final LocalDate TOTAL_PERIOD_START_DATE = LocalDate.of(2000, 1, 1);
    private static final long CARBON_KG_PER_COMPLETED_BOOK = 2L;
    private static final double TREE_PER_COMPLETED_BOOK = 0.04;
    private static final int TOTAL_TREND_MONTH_COUNT = 12;

    private final BookRentalRepository bookRentalRepository;
    private final ReadingProgressRepository readingProgressRepository;
    private final ReviewRepository reviewRepository;
    private final FavoriteBookRepository favoriteBookRepository;

    public UserReadingStatisticResponse getStatistics(Long memberId, String periodType, LocalDate baseDate) {
        LocalDate resolvedBaseDate = baseDate == null ? LocalDate.now() : baseDate;
        PeriodRange periodRange = resolvePeriodRange(periodType, resolvedBaseDate);
        LocalDateTime from = periodRange.startDateTimeInclusive();
        LocalDateTime to = periodRange.endDateTimeExclusive();

        long rentalCount = bookRentalRepository.countByMemberAndPeriod(memberId, from, to);
        long readBookCount = readingProgressRepository.countCompletedByMemberAndPeriod(memberId, from, to);
        long readPageCount = readingProgressRepository.sumReadPageCountByMemberAndPeriod(memberId, from, to);
        long reviewCount = reviewRepository.countByMemberAndPeriod(memberId, Review.STATUS_ACTIVE, from, to);
        long favoriteCount = favoriteBookRepository.countByMember_MemberId(memberId);

        long carbonSavedKg = readBookCount * CARBON_KG_PER_COMPLETED_BOOK;
        double treeSavedCount = readBookCount * TREE_PER_COMPLETED_BOOK;

        List<TrendBucket> buckets = resolveTrendBuckets(periodType, resolvedBaseDate);
        boolean dailyGranularity = isDailyTrendGranularity(periodType);
        LocalDateTime trendFrom = buckets.get(0).startDateTimeInclusive();
        LocalDateTime trendTo = buckets.get(buckets.size() - 1).endDateTimeExclusive();

        Map<String, Long> completedCountByLabel = dailyGranularity
                ? toBucketMap(readingProgressRepository.groupByDayCompletedByMember(memberId, trendFrom, trendTo),
                        b -> b.getBucketDate().toString(), ReadingProgressRepository.DailyCountBucket::getBucketCount)
                : toBucketMap(readingProgressRepository.groupByMonthCompletedByMember(memberId, trendFrom, trendTo),
                        ReadingProgressRepository.MonthlyCountBucket::getBucketMonth, ReadingProgressRepository.MonthlyCountBucket::getBucketCount);

        List<UserReadingStatisticResponse.TrendPoint> readingTrend = new ArrayList<>();
        for (TrendBucket bucket : buckets) {
            readingTrend.add(new UserReadingStatisticResponse.TrendPoint(
                    bucket.label(), bucket.periodStartDate(), bucket.periodEndDate(),
                    completedCountByLabel.getOrDefault(bucket.label(), 0L)));
        }

        return new UserReadingStatisticResponse(
                periodType,
                periodRange.periodStartDate(),
                periodRange.periodEndDate(),
                rentalCount,
                readBookCount,
                readPageCount,
                reviewCount,
                favoriteCount,
                carbonSavedKg,
                treeSavedCount,
                readingTrend,
                LocalDateTime.now());
    }

    // 기간 계산 기준은 관리자 통계(AdminStatisticService.resolvePeriodRange)와 동일하다.
    // 과설계 방지를 위해 공통 유틸로 추출하지 않고 복붙했다(구현계획 §5.3).
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
        if (PERIOD_TYPE_TOTAL.equals(periodType)) {
            // TOTAL: baseDate가 있어도 집계 기간에는 영향을 주지 않는다. periodEndDate는 요청 시점의 오늘 날짜.
            return new PeriodRange(TOTAL_PERIOD_START_DATE, LocalDate.now(), null, null);
        }
        // 컨트롤러의 @Pattern은 HTTP 경로에서만 동작하므로, 서비스 직접 호출(재사용/테스트) 경로에서도
        // 잘못된 periodType이 조용히 TOTAL로 처리되지 않도록 여기서도 fail-fast한다(관리자 통계와 동일 정책).
        throw new BusinessException(ErrorCode.VALIDATION_ERROR);
    }

    // WEEKLY/MONTHLY는 일별 bucket, YEARLY/TOTAL은 월별 bucket이다(resolveTrendBuckets와 동일 기준).
    private boolean isDailyTrendGranularity(String periodType) {
        return PERIOD_TYPE_WEEKLY.equals(periodType) || PERIOD_TYPE_MONTHLY.equals(periodType);
    }

    // GROUP BY 쿼리 결과(List<T>)를 bucket label(일별: yyyy-MM-dd, 월별: yyyy-MM) 기준 Map으로 변환한다.
    private <T> Map<String, Long> toBucketMap(List<T> rows, Function<T, String> labelFn, Function<T, Long> valueFn) {
        Map<String, Long> map = new HashMap<>();
        for (T row : rows) {
            map.put(labelFn.apply(row), valueFn.apply(row));
        }
        return map;
    }

    // 추이 bucket 기준(관리자 통계와 동일): WEEKLY=7일 일별, MONTHLY=해당 월 일별,
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

    private record TrendBucket(String label, LocalDate periodStartDate, LocalDate periodEndDate,
                                LocalDateTime startDateTimeInclusive, LocalDateTime endDateTimeExclusive) {
    }
}
