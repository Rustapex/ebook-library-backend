package com.acorn.abc.admin.statistic.controller;

import com.acorn.abc.common.response.ApiResponse;
import com.acorn.abc.statistic.dto.AdminStatisticResponse;
import com.acorn.abc.statistic.service.AdminStatisticService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "AdminStatistic", description = "관리자 통계 API")
@RestController
@RequestMapping("/api/v1/admin/statistics")
@RequiredArgsConstructor
@Validated
public class AdminStatisticController {

    private final AdminStatisticService adminStatisticService;

    @Operation(summary = "관리자 통계 조회",
            description = "기간·나이대별 KPI, 대여/결제/완독 추이, 탄소·나무 환경 지표를 반환합니다. "
                    + "periodType=TOTAL/WEEKLY/MONTHLY/YEARLY, ageBand=ALL/10S/20S/30S/40S/50_PLUS 전 조합을 요청 시점에 실시간으로 집계합니다. "
                    + "회원 수·제재 회원 수·이용 가능 도서 수는 과거 시점 복원이 어려운 현재 상태 지표라 기간 필터와 무관하게 조회 시점 기준입니다. "
                    + "추이(rentalTrend/paymentTrend/completedReadTrend)는 원본 이벤트 시간 기준 실시간 집계이며, "
                    + "WEEKLY는 7일 일별, MONTHLY는 해당 월 일별, YEARLY는 12개월 월별, TOTAL은 baseDate가 속한 달까지의 최근 12개월 월별 bucket입니다. "
                    + "과거 상태 스냅샷 그래프는 이번 범위에 포함하지 않으며 후속 검토 대상입니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<AdminStatisticResponse>> getStatistics(
            @RequestParam
            @Pattern(regexp = "WEEKLY|MONTHLY|YEARLY|TOTAL", message = "periodType은 WEEKLY, MONTHLY, YEARLY, TOTAL 중 하나여야 합니다.")
            String periodType,
            @Parameter(description = "집계 기준일입니다. WEEKLY/MONTHLY/YEARLY는 이 날짜가 속한 주/월/연도를 집계 기간으로 사용합니다. "
                    + "생략하면 오늘 날짜를 기준일로 사용합니다. TOTAL의 집계 기간(전체)에는 영향을 주지 않지만, "
                    + "TOTAL의 추이 bucket(최근 12개월)과 ageBand의 만 나이 계산 기준일로는 사용됩니다.")
            @RequestParam(required = false) LocalDate baseDate,
            @RequestParam(required = false)
            @Pattern(regexp = "ALL|10S|20S|30S|40S|50_PLUS", message = "ageBand 값이 올바르지 않습니다.")
            String ageBand) {
        AdminStatisticResponse response = adminStatisticService.getStatistics(periodType, baseDate, ageBand);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
