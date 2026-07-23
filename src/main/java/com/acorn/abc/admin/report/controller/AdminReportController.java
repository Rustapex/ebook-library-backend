package com.acorn.abc.admin.report.controller;

import com.acorn.abc.admin.report.dto.AdminReportProcessRequest;
import com.acorn.abc.admin.report.dto.AdminReportProcessResponse;
import com.acorn.abc.admin.report.dto.AdminReportSummaryResponse;
import com.acorn.abc.admin.report.service.AdminReportService;
import com.acorn.abc.common.response.ApiResponse;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.common.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AdminReport", description = "관리자 신고 API")
@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
@Validated
public class AdminReportController {

    private final AdminReportService adminReportService;

    @Operation(summary = "신고 목록 조회", description = "책/리뷰 신고 목록을 조회합니다. 회원 대상 신고는 없습니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminReportSummaryResponse>>> getReports(
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상이어야 합니다.") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size는 1 이상이어야 합니다.") int size) {
        PageResponse<AdminReportSummaryResponse> response = adminReportService.getAdminReports(targetType, status, page, size);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "신고 처리", description = "관리자가 신고를 수동으로 처리합니다. REVIEW 신고의 DONE 처리에서만 리뷰 숨김·계정 정지가 가능합니다.")
    @PatchMapping("/{targetType}/{reportId}/status")
    public ResponseEntity<ApiResponse<AdminReportProcessResponse>> processReport(
            @PathVariable String targetType,
            @PathVariable Long reportId,
            @Valid @RequestBody AdminReportProcessRequest request,
            @AuthenticationPrincipal CustomUserDetails adminDetails) {
        AdminReportProcessResponse response =
                adminReportService.processReport(targetType, reportId, request, adminDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
