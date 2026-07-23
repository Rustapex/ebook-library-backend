package com.acorn.abc.rental.controller;

import com.acorn.abc.common.response.ApiResponse;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.rental.dto.AdminRentalSummaryResponse;
import com.acorn.abc.rental.service.AdminRentalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AdminRental", description = "관리자 대여 API")
@RestController
@RequestMapping("/api/v1/admin/rentals")
@RequiredArgsConstructor
@Validated
public class AdminRentalController {

    private final AdminRentalService adminRentalService;

    @Operation(summary = "대여 현황 조회", description = "회원/도서 검색, 상태 필터로 대여 현황과 진행률·결제 요약을 조회합니다. 조회 전용입니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminRentalSummaryResponse>>> getRentals(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상이어야 합니다.") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size는 1 이상이어야 합니다.") int size) {
        PageResponse<AdminRentalSummaryResponse> response = adminRentalService.getAdminRentals(q, memberId, status, page, size);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
