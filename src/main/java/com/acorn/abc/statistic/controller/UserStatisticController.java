package com.acorn.abc.statistic.controller;

import com.acorn.abc.common.response.ApiResponse;
import com.acorn.abc.common.security.CustomUserDetails;
import com.acorn.abc.statistic.dto.UserReadingStatisticResponse;
import com.acorn.abc.statistic.service.UserStatisticService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "UserStatistic", description = "회원 독서 통계 API")
@RestController
@RequestMapping("/api/v1/me/statistics")
@RequiredArgsConstructor
@Validated
public class UserStatisticController {

    private final UserStatisticService userStatisticService;

    @Operation(summary = "내 독서 통계 조회",
            description = "본인의 기간별 대여/완독/읽은 페이지/리뷰 수, 즐겨찾기 수, 탄소·나무 환경 지표, 완독 추이를 반환합니다. "
                    + "periodType=WEEKLY/MONTHLY/YEARLY/TOTAL을 요청 시점에 실시간으로 집계합니다. "
                    + "즐겨찾기 수는 현재 상태 기준이라 기간과 무관하게 항상 같은 값입니다. "
                    + "읽은 페이지 수는 기간 내 최근 진행 갱신(last_read_at) 시점의 누적 도달 페이지 합이며, 정확한 순수 독서량이 아니라 근사치입니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<UserReadingStatisticResponse>> getMyStatistics(
            @RequestParam
            @Pattern(regexp = "WEEKLY|MONTHLY|YEARLY|TOTAL", message = "periodType은 WEEKLY, MONTHLY, YEARLY, TOTAL 중 하나여야 합니다.")
            String periodType,
            @Parameter(description = "집계 기준일입니다. WEEKLY/MONTHLY/YEARLY는 이 날짜가 속한 주/월/연도를 집계 기간으로 사용합니다. "
                    + "생략하면 오늘 날짜를 기준일로 사용합니다. TOTAL의 집계 기간(전체)에는 영향을 주지 않지만, "
                    + "TOTAL의 추이 bucket(최근 12개월) 기준일로는 사용됩니다.")
            @RequestParam(required = false) LocalDate baseDate,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UserReadingStatisticResponse response =
                userStatisticService.getStatistics(userDetails.getMemberId(), periodType, baseDate);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
