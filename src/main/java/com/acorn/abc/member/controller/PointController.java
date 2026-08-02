package com.acorn.abc.member.controller;

import com.acorn.abc.common.response.ApiResponse;
import com.acorn.abc.common.security.CustomUserDetails;
import com.acorn.abc.member.dto.PointSummaryResponse;
import com.acorn.abc.member.service.PointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Point", description = "포인트 API")
@RestController
@RequestMapping("/api/v1/me/points")
@RequiredArgsConstructor
@Validated
public class PointController {

    private final PointService pointService;

    @Operation(summary = "내 포인트 이력 조회", description = "포인트 잔액과 이력 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PointSummaryResponse>> getPointHistory(
            @RequestParam(required = false) String pointType,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상이어야 합니다.") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size는 1 이상이어야 합니다.") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        PointSummaryResponse response = pointService.getPointHistory(userDetails.getMemberId(), pointType, page, size);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
