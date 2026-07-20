package com.acorn.abc.admin.member.controller;

import com.acorn.abc.admin.member.dto.AdminPointAdjustRequest;
import com.acorn.abc.admin.member.dto.AdminPointAdjustResponse;
import com.acorn.abc.common.response.ApiResponse;
import com.acorn.abc.common.security.CustomUserDetails;
import com.acorn.abc.member.service.AdminPointAdjustService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AdminMember", description = "관리자 회원 API")
@RestController
@RequestMapping("/api/v1/admin/members")
@RequiredArgsConstructor
public class AdminMemberPointController {

    private final AdminPointAdjustService adminPointAdjustService;

    @Operation(summary = "회원 포인트 조정", description = "포인트를 지급/차감하고 이력과 잔액을 반환합니다. requestId는 회원별 멱등 키입니다.")
    @PostMapping("/{memberId}/points")
    public ResponseEntity<ApiResponse<AdminPointAdjustResponse>> adjustPoint(
            @PathVariable Long memberId,
            @Valid @RequestBody AdminPointAdjustRequest request,
            @AuthenticationPrincipal CustomUserDetails adminDetails) {
        AdminPointAdjustResponse response = adminPointAdjustService.adjustPoint(
                memberId, request.requestId(), request.pointAmount(), request.description(), adminDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
