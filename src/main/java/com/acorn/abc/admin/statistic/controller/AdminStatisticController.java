package com.acorn.abc.admin.statistic.controller;

import com.acorn.abc.common.response.ApiResponse;
import com.acorn.abc.statistic.dto.AdminStatisticResponse;
import com.acorn.abc.statistic.service.AdminStatisticService;
import io.swagger.v3.oas.annotations.Operation;
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

    @Operation(summary = "관리자 통계 조회", description = "기간·나이대별 KPI 통계를 반환합니다. 1단계 배치는 TOTAL/ALL 조합만 채웁니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<AdminStatisticResponse>> getStatistics(
            @RequestParam
            @Pattern(regexp = "WEEKLY|MONTHLY|YEARLY|TOTAL", message = "periodType은 WEEKLY, MONTHLY, YEARLY, TOTAL 중 하나여야 합니다.")
            String periodType,
            @RequestParam(required = false) LocalDate baseDate,
            @RequestParam(required = false)
            @Pattern(regexp = "ALL|10S|20S|30S|40S|50_PLUS", message = "ageBand 값이 올바르지 않습니다.")
            String ageBand) {
        AdminStatisticResponse response = adminStatisticService.getStatistics(periodType, baseDate, ageBand);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
