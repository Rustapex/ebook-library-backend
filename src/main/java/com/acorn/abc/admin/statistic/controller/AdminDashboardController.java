package com.acorn.abc.admin.statistic.controller;

import com.acorn.abc.common.response.ApiResponse;
import com.acorn.abc.statistic.dto.AdminDashboardResponse;
import com.acorn.abc.statistic.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AdminDashboard", description = "관리자 대시보드 API")
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @Operation(summary = "관리자 대시보드 조회",
            description = "KPI 통계와 최근 결제 목록을 반환합니다. "
                    + "1차 구현으로 최근 결제만 포함하며, 최근 신고·최근 희망도서 위젯은 review/request 도메인 Repository 추가 후 후속 작업으로 제공될 예정입니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.ok(adminDashboardService.getDashboard()));
    }
}
