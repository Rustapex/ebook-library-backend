package com.acorn.abc.payment.controller;

import com.acorn.abc.common.response.ApiResponse;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.payment.dto.AdminPaymentSummaryResponse;
import com.acorn.abc.payment.service.AdminPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "AdminPayment", description = "관리자 결제 API")
@RestController
@RequestMapping("/api/v1/admin/payments")
@RequiredArgsConstructor
@Validated
public class AdminPaymentController {

    private final AdminPaymentService adminPaymentService;

    @Operation(summary = "결제 목록 조회", description = "회원/도서 검색, 결제일 범위로 PAID 결제 목록과 쿠폰/포인트 차감 상세를 조회합니다. 취소/환불/복구는 제공하지 않습니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminPaymentSummaryResponse>>> getPayments(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상이어야 합니다.") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size는 1 이상이어야 합니다.") int size) {
        PageResponse<AdminPaymentSummaryResponse> response = adminPaymentService.getAdminPayments(q, from, to, page, size);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
