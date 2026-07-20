package com.acorn.abc.admin.notification.controller;

import com.acorn.abc.admin.notification.dto.AdminNoticeCreateResponse;
import com.acorn.abc.admin.notification.dto.AdminNoticeResponse;
import com.acorn.abc.admin.notification.dto.AdminNoticeSaveRequest;
import com.acorn.abc.common.response.ApiResponse;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.common.security.CustomUserDetails;
import com.acorn.abc.notification.service.AdminNoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AdminNotice", description = "관리자 공지 API")
@RestController
@RequestMapping("/api/v1/admin/notices")
@RequiredArgsConstructor
public class AdminNoticeController {

    private final AdminNoticeService adminNoticeService;

    @Operation(summary = "공지 목록 조회", description = "ACTIVE/HIDDEN 상태를 모두 조회할 수 있습니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminNoticeResponse>>> getAdminNotices(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return ResponseEntity.ok(ApiResponse.ok(adminNoticeService.getAdminNotices(status, page, size)));
    }

    @Operation(summary = "공지 등록", description = "notifyYn=true인 신규 등록에만 대상 회원에게 알림을 발송합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<AdminNoticeCreateResponse>> createNotice(
            @Valid @RequestBody AdminNoticeSaveRequest request,
            @AuthenticationPrincipal CustomUserDetails adminDetails
    ) {
        AdminNoticeCreateResponse response = adminNoticeService.createNotice(adminDetails.getMemberId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @Operation(summary = "공지 수정/숨김", description = "기존 공지를 수정합니다. 알림은 재발송하지 않습니다.")
    @PutMapping("/{noticeId}")
    public ResponseEntity<ApiResponse<AdminNoticeResponse>> updateNotice(
            @PathVariable Long noticeId,
            @Valid @RequestBody AdminNoticeSaveRequest request,
            @AuthenticationPrincipal CustomUserDetails adminDetails
    ) {
        AdminNoticeResponse response = adminNoticeService.updateNotice(adminDetails.getMemberId(), noticeId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
