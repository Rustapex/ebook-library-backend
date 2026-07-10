package com.acorn.abc.statistic.service;

import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.statistic.domain.AdminStatistic;
import com.acorn.abc.statistic.dto.AdminStatisticResponse;
import com.acorn.abc.statistic.repository.AdminStatisticRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AdminStatisticServiceTest {

    @Mock
    private AdminStatisticRepository adminStatisticRepository;

    @InjectMocks
    private AdminStatisticService adminStatisticService;

    @Test
    void getStatistics_TOTAL_ALL_조합은_저장된_값을_반환한다() {
        AdminStatistic statistic = AdminStatistic.create(
                "TOTAL", "ALL", AdminStatisticBatchService.TOTAL_PERIOD_START_DATE, LocalDate.now());
        given(adminStatisticRepository.findByPeriodTypeAndPeriodStartDateAndAgeBand(
                "TOTAL", AdminStatisticBatchService.TOTAL_PERIOD_START_DATE, "ALL"))
                .willReturn(Optional.of(statistic));

        AdminStatisticResponse response = adminStatisticService.getStatistics("TOTAL", null, null);

        assertThat(response.periodType()).isEqualTo("TOTAL");
        assertThat(response.ageBand()).isEqualTo("ALL");
    }

    @Test
    void getStatistics_WEEKLY처럼_아직_배치가_안_채운_조합은_COMMON_NOT_FOUND() {
        assertThatThrownBy(() -> adminStatisticService.getStatistics("WEEKLY", null, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.COMMON_NOT_FOUND);
    }

    @Test
    void getStatistics_TOTAL이어도_ageBand이_ALL이_아니면_COMMON_NOT_FOUND() {
        assertThatThrownBy(() -> adminStatisticService.getStatistics("TOTAL", null, "10S"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.COMMON_NOT_FOUND);
    }

    @Test
    void getStatistics_TOTAL_ALL이어도_배치가_아직_실행_전이면_COMMON_NOT_FOUND() {
        given(adminStatisticRepository.findByPeriodTypeAndPeriodStartDateAndAgeBand(
                "TOTAL", AdminStatisticBatchService.TOTAL_PERIOD_START_DATE, "ALL"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> adminStatisticService.getStatistics("TOTAL", null, "ALL"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.COMMON_NOT_FOUND);
    }
}
