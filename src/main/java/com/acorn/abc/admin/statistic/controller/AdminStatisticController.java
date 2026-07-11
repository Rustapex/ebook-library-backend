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
            description = "기간·나이대별 KPI 통계를 반환합니다. "
                    + "현재는 periodType=TOTAL, ageBand=ALL(또는 미지정) 조합만 지원하며, 요청 시점에 실시간으로 집계합니다. "
                    + "그 외 조합은 404(COMMON_NOT_FOUND)를 반환합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<AdminStatisticResponse>> getStatistics(
            @RequestParam
            @Pattern(regexp = "WEEKLY|MONTHLY|YEARLY|TOTAL", message = "periodType은 WEEKLY, MONTHLY, YEARLY, TOTAL 중 하나여야 합니다.")
            String periodType,
            @Parameter(description = "현재 미사용 파라미터입니다. WEEKLY/MONTHLY/YEARLY 기간별 조회를 지원하는 후속 작업에서 사용될 예정입니다.")
            @RequestParam(required = false) LocalDate baseDate,
            @RequestParam(required = false)
            @Pattern(regexp = "ALL|10S|20S|30S|40S|50_PLUS", message = "ageBand 값이 올바르지 않습니다.")
            String ageBand) {
        AdminStatisticResponse response = adminStatisticService.getStatistics(periodType, baseDate, ageBand);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
