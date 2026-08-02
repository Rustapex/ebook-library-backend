package com.acorn.abc.member.controller;

import com.acorn.abc.common.response.ApiResponse;
import com.acorn.abc.common.security.CustomUserDetails;
import com.acorn.abc.member.dto.AttendanceCheckResponse;
import com.acorn.abc.member.dto.AttendanceMonthResponse;
import com.acorn.abc.member.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@Tag(name = "Attendance", description = "출석 API")
@RestController
@RequestMapping("/api/v1/me/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @Operation(summary = "출석 체크", description = "오늘 출석을 저장하고 연속 출석일수를 갱신합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<AttendanceCheckResponse>> checkIn(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        AttendanceCheckResponse response = attendanceService.checkIn(userDetails.getMemberId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @Operation(summary = "출석 현황 조회", description = "월별 출석일, 오늘 출석 여부, 연속 출석일수를 반환합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<AttendanceMonthResponse>> getMonthlyAttendance(
            @RequestParam("yearMonth") @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        AttendanceMonthResponse response = attendanceService.getMonthlyAttendance(userDetails.getMemberId(), yearMonth);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
