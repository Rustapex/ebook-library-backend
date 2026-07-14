package com.acorn.abc.statistic.service;

import com.acorn.abc.favorite.repository.FavoriteBookRepository;
import com.acorn.abc.rental.repository.BookRentalRepository;
import com.acorn.abc.rental.repository.ReadingProgressRepository;
import com.acorn.abc.review.domain.Review;
import com.acorn.abc.review.repository.ReviewRepository;
import com.acorn.abc.statistic.dto.UserReadingStatisticResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 회원 본인 독서 통계를 실시간으로 집계한다(회원용독서통계 구현계획 §4.1 — 관리자 통계와 동일한
 * 실시간 집계 정책 적용, user_reading_statistic 테이블/배치는 쓰지 않는다).
 * "읽은 페이지"는 last_read_at 기간 필터 기준 근사치다(§4.2, B1 확정).
 * 즐겨찾기 수는 기간과 무관한 현재 상태 지표다(§4.4 확정).
 * 기간/추이 계산은 AdminStatisticService와 공통인 StatisticPeriodResolver/TrendBucketCalculator를 사용한다(관리자API+사용자통계 전체잔여작업 §3.1-2 확정).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserStatisticService {

    private static final long CARBON_KG_PER_COMPLETED_BOOK = 2L;
    private static final double TREE_PER_COMPLETED_BOOK = 0.04;

    private final BookRentalRepository bookRentalRepository;
    private final ReadingProgressRepository readingProgressRepository;
    private final ReviewRepository reviewRepository;
    private final FavoriteBookRepository favoriteBookRepository;
    private final StatisticPeriodResolver periodResolver;
    private final TrendBucketCalculator trendBucketCalculator;

    public UserReadingStatisticResponse getStatistics(Long memberId, String periodType, LocalDate baseDate) {
        LocalDate resolvedBaseDate = baseDate == null ? LocalDate.now() : baseDate;
        StatisticPeriodResolver.PeriodRange periodRange = periodResolver.resolvePeriodRange(periodType, resolvedBaseDate);
        LocalDateTime from = periodRange.startDateTimeInclusive();
        LocalDateTime to = periodRange.endDateTimeExclusive();

        long rentalCount = bookRentalRepository.countByMemberAndPeriod(memberId, from, to);
        long readBookCount = readingProgressRepository.countCompletedByMemberAndPeriod(memberId, from, to);
        long readPageCount = readingProgressRepository.sumReadPageCountByMemberAndPeriod(memberId, from, to);
        long reviewCount = reviewRepository.countByMemberAndPeriod(memberId, Review.STATUS_ACTIVE, from, to);
        long favoriteCount = favoriteBookRepository.countByMember_MemberId(memberId);

        long carbonSavedKg = readBookCount * CARBON_KG_PER_COMPLETED_BOOK;
        double treeSavedCount = readBookCount * TREE_PER_COMPLETED_BOOK;

        List<TrendBucketCalculator.TrendBucket> buckets = trendBucketCalculator.resolveBuckets(periodType, resolvedBaseDate);
        boolean dailyGranularity = trendBucketCalculator.isDailyGranularity(periodType);
        LocalDateTime trendFrom = buckets.get(0).startDateTimeInclusive();
        LocalDateTime trendTo = buckets.get(buckets.size() - 1).endDateTimeExclusive();

        Map<String, Long> completedCountByLabel = dailyGranularity
                ? trendBucketCalculator.toBucketMap(readingProgressRepository.groupByDayCompletedByMember(memberId, trendFrom, trendTo),
                        b -> b.getBucketDate().toString(), ReadingProgressRepository.DailyCountBucket::getBucketCount)
                : trendBucketCalculator.toBucketMap(readingProgressRepository.groupByMonthCompletedByMember(memberId, trendFrom, trendTo),
                        ReadingProgressRepository.MonthlyCountBucket::getBucketMonth, ReadingProgressRepository.MonthlyCountBucket::getBucketCount);

        List<UserReadingStatisticResponse.TrendPoint> readingTrend = new ArrayList<>();
        for (TrendBucketCalculator.TrendBucket bucket : buckets) {
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
}
