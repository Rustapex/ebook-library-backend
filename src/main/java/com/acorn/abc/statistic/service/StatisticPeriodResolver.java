package com.acorn.abc.statistic.service;

import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

/**
 * 관리자/회원 통계 공통 기간 계산 컴포넌트(관리자API+사용자통계 전체잔여작업 구현계획 §3.1-2 확정).
 * {@code AdminStatisticService}와 {@code UserStatisticService}가 각자 들고 있던 동일한
 * periodType(WEEKLY/MONTHLY/YEARLY/TOTAL) 기간 계산 로직을 통합했다.
 */
@Component
public class StatisticPeriodResolver {

    private static final String PERIOD_TYPE_WEEKLY = "WEEKLY";
    private static final String PERIOD_TYPE_MONTHLY = "MONTHLY";
    private static final String PERIOD_TYPE_YEARLY = "YEARLY";
    private static final String PERIOD_TYPE_TOTAL = "TOTAL";
    // TOTAL 통계의 시작일 표기용 고정값. 실제 서비스 시작일이 아니라 "전체 기간"을 의미하는 상수다.
    private static final LocalDate TOTAL_PERIOD_START_DATE = LocalDate.of(2000, 1, 1);

    // 기간 계산 기준(관리자 통계 확장 §3.1). start <= timestamp < endExclusive로 통일한다.
    public PeriodRange resolvePeriodRange(String periodType, LocalDate baseDate) {
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
        // 호출측 컨트롤러의 @Pattern은 HTTP 경로에서만 동작하므로, 서비스 직접 호출(재사용/테스트) 경로에서도
        // 잘못된 periodType이 조용히 TOTAL로 처리되지 않도록 여기서도 fail-fast한다.
        throw new BusinessException(ErrorCode.VALIDATION_ERROR);
    }

    public record PeriodRange(LocalDate periodStartDate, LocalDate periodEndDate,
                               LocalDateTime startDateTimeInclusive, LocalDateTime endDateTimeExclusive) {
    }
}
