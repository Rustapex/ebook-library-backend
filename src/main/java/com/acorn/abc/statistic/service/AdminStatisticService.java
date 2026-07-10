package com.acorn.abc.statistic.service;

import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.statistic.domain.AdminStatistic;
import com.acorn.abc.statistic.dto.AdminStatisticResponse;
import com.acorn.abc.statistic.repository.AdminStatisticRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStatisticService {

    private static final String PERIOD_TYPE_TOTAL = "TOTAL";
    private static final String AGE_BAND_ALL = "ALL";

    private final AdminStatisticRepository adminStatisticRepository;

    public AdminStatisticResponse getStatistics(String periodType, LocalDate baseDate, String ageBand) {
        String resolvedAgeBand = ageBand == null ? AGE_BAND_ALL : ageBand;
        return AdminStatisticResponse.from(findStatistic(periodType, resolvedAgeBand));
    }

    public AdminStatistic getTotalStatisticForDashboard() {
        return findStatistic(PERIOD_TYPE_TOTAL, AGE_BAND_ALL);
    }

    private AdminStatistic findStatistic(String periodType, String ageBand) {
        // 1단계 배치(구현계획 §4-1)는 TOTAL+ALL 조합만 채운다.
        // 그 외 조합은 아직 집계되지 않아 COMMON_NOT_FOUND로 응답한다 — 응답 정책은 §9(남은 작업)에서 재검토 예정.
        if (!PERIOD_TYPE_TOTAL.equals(periodType) || !AGE_BAND_ALL.equals(ageBand)) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND);
        }
        return adminStatisticRepository
                .findByPeriodTypeAndPeriodStartDateAndAgeBand(
                        periodType, AdminStatisticBatchService.TOTAL_PERIOD_START_DATE, ageBand)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_NOT_FOUND));
    }
}
