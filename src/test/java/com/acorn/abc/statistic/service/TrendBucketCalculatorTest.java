package com.acorn.abc.statistic.service;

import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PR #46 리뷰 반영: resolveBuckets()가 TOTAL을 명시 분기하고, 그 외 값은 조용히
 * TOTAL로 처리되지 않고 fail-fast하는지 확인한다(StatisticPeriodResolver와 동일 정책).
 */
class TrendBucketCalculatorTest {

    private final TrendBucketCalculator trendBucketCalculator = new TrendBucketCalculator();

    @Test
    void resolveBuckets_TOTAL은_최근_12개월_월별_bucket을_반환한다() {
        List<TrendBucketCalculator.TrendBucket> buckets =
                trendBucketCalculator.resolveBuckets("TOTAL", LocalDate.of(2026, 7, 15));

        assertThat(buckets).hasSize(12);
        assertThat(buckets.get(11).label()).isEqualTo("2026-07");
    }

    @Test
    void resolveBuckets_잘못된_periodType은_VALIDATION_ERROR를_던진다() {
        assertThatThrownBy(() -> trendBucketCalculator.resolveBuckets("DAILY", LocalDate.of(2026, 7, 15)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }
}
