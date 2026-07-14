package com.acorn.abc.statistic.service;

import org.springframework.stereotype.Component;

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
 * 관리자/회원 통계 공통 추이(trend) bucket 계산 컴포넌트(관리자API+사용자통계 전체잔여작업 구현계획 §3.1-2 확정).
 * {@code AdminStatisticService}와 {@code UserStatisticService}가 각자 들고 있던 동일한 bucket 계산 로직을 통합했다.
 */
@Component
public class TrendBucketCalculator {

    private static final String PERIOD_TYPE_WEEKLY = "WEEKLY";
    private static final String PERIOD_TYPE_MONTHLY = "MONTHLY";
    private static final String PERIOD_TYPE_YEARLY = "YEARLY";
    private static final int TOTAL_TREND_MONTH_COUNT = 12;

    // WEEKLY/MONTHLY는 일별 bucket, YEARLY/TOTAL은 월별 bucket이다(resolveBuckets와 동일 기준).
    public boolean isDailyGranularity(String periodType) {
        return PERIOD_TYPE_WEEKLY.equals(periodType) || PERIOD_TYPE_MONTHLY.equals(periodType);
    }

    // 추이 bucket 기준(관리자 통계 확장 §3.4, §3.6 확정): WEEKLY=7일 일별, MONTHLY=해당 월 일별,
    // YEARLY=12개월 월별, TOTAL=baseDate가 속한 달까지의 최근 12개월 월별.
    public List<TrendBucket> resolveBuckets(String periodType, LocalDate baseDate) {
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

    // GROUP BY 쿼리 결과(List<T>)를 bucket label(일별: yyyy-MM-dd, 월별: yyyy-MM) 기준 Map으로 변환한다.
    public <T> Map<String, Long> toBucketMap(List<T> rows, Function<T, String> labelFn, Function<T, Long> valueFn) {
        Map<String, Long> map = new HashMap<>();
        for (T row : rows) {
            map.put(labelFn.apply(row), valueFn.apply(row));
        }
        return map;
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

    public record TrendBucket(String label, LocalDate periodStartDate, LocalDate periodEndDate,
                               LocalDateTime startDateTimeInclusive, LocalDateTime endDateTimeExclusive) {
    }
}
