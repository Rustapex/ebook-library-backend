package com.acorn.abc.admin.statistic.controller;

import com.acorn.abc.common.response.ApiResponse;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.statistic.dto.AdminAuditLogSummaryResponse;
import com.acorn.abc.statistic.service.AdminAuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AdminAuditLog", description = "관리자 감사 로그 API")
@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
@Validated
public class AdminAuditLogController {

    private final AdminAuditLogService adminAuditLogService;

    @Operation(summary = "감사 로그 목록 조회", description = "관리자 작업 이력을 조회합니다. 조회 전용이며 수정/삭제 API는 제공하지 않습니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminAuditLogSummaryResponse>>> getAuditLogs(
            @Parameter(description = "관리자 작업 유형 필터입니다. 생략 가능. 허용값: "
                    + "MEMBER_POINT_ADJUST, COUPON_CREATE, COUPON_ISSUE, BOOK_CREATE, CHALLENGE_UPDATE, "
                    + "MEMBER_UPDATE, BOOK_UPDATE, REPORT_PROCESS, NOTICE_WRITE. 목록에 없는 값은 400을 반환합니다.")
            @RequestParam(required = false) String actionType,
            @Parameter(description = "감사 로그 대상 유형 필터입니다. 생략 가능. 허용값: "
                    + "MEMBER, COUPON, BOOK, CHALLENGE, BOOK_REPORT, REVIEW_REPORT, REVIEW, NOTICE, SANCTION_HISTORY. "
                    + "목록에 없는 값은 400을 반환합니다.")
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) Long targetId,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상이어야 합니다.") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size는 1 이상이어야 합니다.") int size) {
        PageResponse<AdminAuditLogSummaryResponse> response =
                adminAuditLogService.getAuditLogs(actionType, targetType, targetId, page, size);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
